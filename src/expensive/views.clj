(ns expensive.views
  (:use hiccup.core)
  (:require [clj-time.core :as datetime])
  (:use expensive.util))

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
  (html
    (login-form)))

(defn current-balance-indicator [transactions]
  (let [cash-ts   (filter #(= "cash" (% :source)) transactions)
        cash-bal  (apply + (map #(* (% :amount) (if (= "in" (% :direction)) 1 -1)) cash-ts))
        bank-ts   (filter #(= "bank" (% :source)) transactions)
        bank-bal  (apply + (map #(* (% :amount) (if (= "in" (% :direction)) 1 -1)) bank-ts))]
    [:div.current-balance-indicator
      [:div.container
        [:div.title "Bank"]
        [:div.balance (str bank-bal)]]
      [:div.container
        [:div.title "Cash"]
        [:div.balance (str cash-bal)]]]))

(defn thirty-day-report [transactions]
  [:div.thirty-day-report
    (for [i (range 31)]
      (let [date (datetime/minus (datetime/now) (datetime/days i))]
        [:div.day
          [:h3 (date-for-humans date)]
          (map
            (fn [t]
              [:div.transaction
                [:div.title (t :title)
                [:div.amount (t :amount)]]])
            (transactions (date-for-db date)))]))])

(defn add-transaction-form []
  [:div.add-transaction-form
    [:form {:action "" :method "POST"}
      [:div.field
        [:input#title-field {:type "text" :name "title" :value "Title"}]]
      [:div.field
        [:input#amount-field {:type "text" :name "amount" :value "Amount"}]]
      [:div.field
        [:select#source-field {:name "source"}
          [:option {:value "cash"} "Cash"]
          [:option {:value "bank"} "Bank"]]]
      [:div.field
        [:select#direction-field {:name "direction"}
          [:option {:value "in"} "In"]
          [:option {:value "out"} "Out"]]]
      [:div.button
        [:input.button {:type "submit" :value "Add"}]]]])

(defn index [user]
  (html
    (current-balance-indicator (user :transactions))
    (thirty-day-report (group-by :date (user :transactions)))
    (add-transaction-form)))
