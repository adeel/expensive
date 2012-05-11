(ns expensive.core
  (:use ring.adapter.jetty)
  (:use [ring.util.response :only (redirect)])
  (:use compojure.core)
  (:require [compojure.handler :as handler])
  (:use [sandbar.stateful-session :only (wrap-stateful-session
                                         session-put!
                                         session-get)])
  (:use [hozumi.mongodb-session :only (mongodb-store)])
  (:require [somnium.congomongo :as db])
  (:require [clj-time.core :as datetime])
  (:require [expensive.views :as views])
  (:use [expensive.util :only (mongo object-id sha1 date-for-db)]))

(mongo)

(defroutes main-routes
  (GET "/" []
    (println (session-get :user))
    (if-let [user (db/fetch-one :users :where {:_id (object-id (session-get :user))})]
      (views/index user)
      (views/login)))
  (POST "/login" {user :params}
    (if-let [username (user :username)]
      (if-let [password (user :password)]
        (if-let [user (db/fetch-one :users :where {:username username
                                                   :password (sha1 password)})]
          (do (session-put! :user (str (user :_id)))
              (redirect "/"))
          (redirect "/")))))
  (POST "/" {{:keys [title amount direction source]} :params}
    (if-let [user (db/fetch-one :users :where {:_id (object-id (session-get :user))})]
      (do (db/update! :users user {"$push" {:transactions
            {:title     title
             :amount    (try (Float/parseFloat amount) (catch Exception e 0))
             :source    source
             :direction direction
             :date      (date-for-db (datetime/now))}}})
          (redirect "/"))
      (views/login))))

(defn wrap-with-logger [handler]
  (fn [req]
    (println (str "  " (req :request-method) " " (req :uri)))
    (if-not (empty? (req :params)) (println (str "    " (req :params))))
    (handler req)))

(def app
  (-> main-routes
    wrap-with-logger
    (wrap-stateful-session {:store (mongodb-store)})
    handler/api))

(defn -main []
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "8080"))]
    (run-jetty app {:port port})))