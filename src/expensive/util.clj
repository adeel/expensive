(ns expensive.util
  (:import (java.security MessageDigest))
  (:require [clj-time.core :as datetime]
            [clj-time.coerce :as datetime-coerce]
            [clj-time.format :as datetime-format])
  (:require [somnium.congomongo :as db]))

;;

(defn get-now-as-string []
  (datetime-coerce/to-string (datetime/now)))

(defn date-for-humans [date]
  (datetime-format/unparse (datetime-format/formatter "d MMM Y") date))

(defn date-for-db [date]
  (datetime-format/unparse (datetime-format/formatters :year-month-day) date))

(defn date-from-db-format [string]
  (datetime-format/parse (datetime-format/formatters :year-month-day) string))

;;

(defn sha1 [obj]
   (let [bytes (.getBytes (with-out-str (pr obj)))] 
     (apply vector (.digest (MessageDigest/getInstance "SHA1") bytes))))

;;

(defn object-id [id]
  (try (db/object-id id)
    (catch Exception e nil)))

(defn mongo []
  (let [url (get (System/getenv) "MONGOHQ_URL" "mongodb://127.0.0.1:27017/expensive")
        [_ _ username password host port database] (re-find #"mongodb\:\/\/((.+)\:(.+)\@)?(.+)\:(\d+)\/(.+)" url)
        port (Integer/parseInt port)
        connection (db/make-connection database :host host :port port)]
    (db/set-connection! connection)
    (if (not (and (empty? username) (empty? password)))
      (db/authenticate connection username password))))
