(ns expensive.views
  (:use hiccup.core)
  (:use hiccup.page)
  (:require [clj-time.core :as datetime])
  (:use expensive.util))

(defn layout [& body]
  (html5
    [:head
      [:title "Expensive"]
      [:meta {:content "text/html; charset=UTF-8" :http-equiv "content-type"}]
      [:meta {:name "viewport" :content "width=device-width"}]
      [:meta {:name "apple-mobile-web-app-capable" :content "yes"}]
      (include-css "/style.css")]
    [:body
      body]))

(defn login-form []
  [:div.login-form
    [:form {:action "/login" :method "POST"}
      [:div.field
        [:input.text {:type "text" :name "username" :value "Username"}]]
      [:div.field
        [:input.text {:type "password" :name "password" :value "Password"}]]
      [:div.button
        [:input.button {:type "submit" :value "Login"}]]]])

(defn login []
  (layout
    (login-form)))

(defn current-balance-indicator [transactions]
  (let [cash-ts   (filter #(= "cash" (% :source)) transactions)
        cash-bal  (apply + (map #(* (% :amount) (if (= "in" (% :direction)) 1 -1)) cash-ts))
        bank-ts   (filter #(= "bank" (% :source)) transactions)
        bank-bal  (apply + (map #(* (% :amount) (if (= "in" (% :direction)) 1 -1)) bank-ts))]
    [:div.current-balance-indicator
      [:div.container
        [:div.title "Bank"]
        [:div.balance (format "%.2f" bank-bal)]]
      [:div.container
        [:div.title "Cash"]
        [:div.balance (format "%.2f" cash-bal)]]]))

(defn thirty-day-report [transactions]
  [:div.thirty-day-report
    (for [i (range 31)]
      (let [date (datetime/minus (datetime/now) (datetime/days i))
            ts   (transactions (date-for-db date))]
        (when (> (count ts) 0)
          [:div.day
            [:h3 (date-for-humans date)]
            (map
              (fn [t]
                [:div.transaction {:direction (t :direction)}
                  [:div.title (t :title)]
                  [:div.amount (format "%.2f" (t :amount))]
                  [:div.source (t :source)]])
              ts)])))])

(defn add-transaction-form []
  [:div.add-transaction-form
    [:form {:action "" :method "POST"}
      [:div.field
        [:input#title-field {:type "text" :name "title" :value "Title"}]]
      [:div.field
        [:input {:type "text" :name "amount" :value "Amount"}]]
      [:div#source-field.field
        [:select {:name "source"}
          [:option {:value "cash"} "Cash"]
          [:option {:value "bank"} "Bank"]]]
      [:div#direction-field.field
        [:select {:name "direction"}
          [:option {:value "in"} "In"]
          [:option {:value "out"} "Out"]]]
      [:div.button
        [:input.button {:type "submit" :value "Add"}]]]])

(defn index [user]
  (layout
    (current-balance-indicator (user :transactions))
    (add-transaction-form)
    (thirty-day-report (group-by :date (user :transactions)))))
