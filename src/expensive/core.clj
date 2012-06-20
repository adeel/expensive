(ns expensive.core
  (:use ring.adapter.jetty)
  (:use [ring.util.response :only (redirect)])
  (:use compojure.core)
  (:require [compojure.handler :as handler])
  (:use [compojure.route :only (resources)])
  (:use [sandbar.stateful-session :only (wrap-stateful-session
                                         session-put!
                                         session-get)])
  (:use [hozumi.mongodb-session :only (mongodb-store)])
  (:require [somnium.congomongo :as db])
  (:require [clj-time.core :as datetime])
  (:use [hiccup.middleware :only (wrap-base-url)])
  (:require [clojure-csv.core :as csv])
  (:require [expensive.views :as views])
  (:use [expensive.util :only (mongo object-id sha1 date-for-db date-from-db-format)]))

(mongo)

(defroutes main-routes
  (GET "/" []
    (if-let [user (db/fetch-one :users :where {:_id (object-id (session-get :user))})]
      (let [ts (map #(db/fetch-one :transactions :where {:_id %})
                 (user :transactions))]
        (session-put! :user (session-get :user)) ; force session timeout to extend
        (views/index ts))
      (views/login)))
  (POST "/login" {user :params}
    (if-let [username (user :username)]
      (if-let [password (user :password)]
        (if-let [user (db/fetch-one :users :where {:username username
                                                   :password (sha1 password)})]
          (do (session-put! :user (str (user :_id)))
              (redirect "/"))
          (redirect "/")))))
  (POST "/" {{:keys [category amount direction source date]} :params}
    (if-let [user (db/fetch-one :users :where {:_id (object-id (session-get :user))})]
      (let [date (try (date-from-db-format date)
                   (catch Exception e (date-for-db (datetime/now))))
            t    (db/insert! :transactions
                   {:user      (user :_id)
                    :category  category
                    :amount    (try (Float/parseFloat amount) (catch Exception e 0.0))
                    :source    source
                    :direction direction
                    :date      (date-for-db date)})]
        (db/update! :users user {"$push" {:transactions (t :_id)}})
        (redirect "/"))
      (views/login)))
  (GET "/transactions.csv" []
    (if-let [user (db/fetch-one :users :where {:_id (object-id (session-get :user))})]
      {:headers {"content-type" "text/csv"}
       :body    (csv/write-csv
                  (map
                    (fn [t]
                      [(t :date)
                       (format "%.2f" (float (* (t :amount) (if (= (t :direction) "in") 1 -1))))
                       (t :category)
                       (t :source)])
                    (db/fetch :transactions :where {:user (user :_id)})))}
      (redirect "/")))
  (GET "/summary.csv" {{:keys [start end]} :params}
    (if-let [user (db/fetch-one :users :where {:_id (object-id (session-get :user))})]
      (let [start (or (try (date-from-db-format start) (catch Exception e nil))
                      (date-from-db-format "1900-01-01"))
            end   (or (try (date-from-db-format end) (catch Exception e nil))
                      (datetime/now))
            ts    (db/fetch :transactions :where
                    {:user (user :_id)
                     :date {"$gte" (date-for-db start)
                            "$lte" (date-for-db end)}})]
        {:headers {"content-type" "text/csv"}
         :body    (csv/write-csv
                    (let [grouped (group-by :category ts)]
                      (reduce
                        (fn [m cat]
                          (assoc m cat
                            (str
                              (apply +
                                (map
                                  (fn [t]
                                    (* (t :amount)
                                       (if (= (t :direction) "in") 1 -1)))
                                  (grouped cat))))))
                        {}
                        (keys grouped))))})
      (redirect "/")))
  (resources "/"))

(defn wrap-with-logger [handler]
  (fn [req]
    (println (str "  " (req :request-method) " " (req :uri)))
    (if-not (empty? (req :params)) (println (str "    " (req :params))))
    (handler req)))

(def app
  (-> main-routes
    wrap-with-logger
    wrap-base-url
    (wrap-stateful-session {:store            (mongodb-store)
                            :auto-key-change? true})
    handler/api))

(defn -main []
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "8080"))]
    (run-jetty app {:port port})))