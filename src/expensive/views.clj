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
      [:script {:src "//ajax.googleapis.com/ajax/libs/mootools/1.4.5/mootools-yui-compressed.js"}]
      (include-css "/style.css")]
    [:body
      body]))

(defn login-form []
  [:div.login-form
    [:form {:action "/login" :method "POST"}
      [:div.field
        [:input.text {:type "text" :name "username" :value "Username" :onfocus "this.value='';"}]]
      [:div.field
        [:input.text {:type "password" :name "password" :value "Password" :onfocus "this.value='';"}]]
      [:div.button
        [:input.button {:type "submit" :value "Login"}]]]])

(defn login []
  (layout
    (login-form)))

(defn current-balance-indicator [transactions]
  (let [cash-ts   (filter #(= "cash" (% :source)) transactions)
        cash-bal  (apply + (map #(* (% :amount) (if (= "in" (% :direction)) 1 -1)) cash-ts))
        bank-ts   (filter #(= "bank" (% :source)) transactions)
        bank-bal  (apply + (map #(* (% :amount) (if (= "in" (% :direction)) 1 -1)) bank-ts))
        save-ts   (filter #(= "savings" (% :source)) transactions)
        save-bal  (apply + (map #(* (% :amount) (if (= "in" (% :direction)) 1 -1)) save-ts))]
    [:div.current-balance-indicator
      [:div.container
        [:div.title "Bank"]
        [:div.balance (format "%.2f" (float bank-bal))]]
      [:div.container
        [:div.title "Cash"]
        [:div.balance (format "%.2f" (float cash-bal))]]
      [:div.container
        [:div.title "Savings"]
        [:div.balance (format "%.2f" (float save-bal))]]]))

(defn transaction-list [transactions delta]
  [:div.transaction-list
    (for [i (range (- delta) (inc delta))]
      (let [date (datetime/minus (datetime/now) (datetime/days i))
            ts   (transactions (date-for-db date))]
        (when (> (count ts) 0)
          [:div.day
            [:h3 (date-for-humans date)]
            [:div.transactions
              (map-indexed
                (fn [i t]
                  [:div.transaction {:id (str (t :_id)) :direction (t :direction) :alternate (= (mod i 2) 1)}
                    [:div.category (t :category)]
                    [:div.amount (format "%.2f" (float (t :amount)))]
                    [:div.source (t :source)]])
                ts)]])))])

(defn n-day-date-selector [start-date n]
  [:select {:name "date"}
    (for [i (range (inc n))
          :let [date (datetime/plus start-date (datetime/days i))]]
      [:option {:value (date-for-db date)} (date-for-humans date)])])

(defn add-transaction-form []
  [:div#add-transaction-form.add-transaction-form.hidden
    [:form {:action "" :method "POST"}
      [:div.field
        [:input#category-field {:type "text" :name "category" :placeholder "Category"}]]
      [:div.field
        [:input {:type "text" :name "amount" :placeholder "Amount"}]]
      [:div#source-field.field
        [:select {:name "source"}
          [:option "Source"]
          [:option {:value "cash"} "Cash"]
          [:option {:value "bank"} "Bank"]
          [:option {:value "savings"} "Savings"]]]
      [:div#direction-field.field
        [:select {:name "direction"}
          [:option "Direction"]
          [:option {:value "in"} "In"]
          [:option {:value "out"} "Out"]]]
      [:div#date-field.field
        (n-day-date-selector (datetime/now) 30)]
      [:div.button
        [:input.button {:type "submit" :value "Add"}]]]])

(defn index [transactions]
  (layout
    (current-balance-indicator transactions)
    [:div.add-transaction-form-toggler
      {:onclick "var f=$('add-transaction-form'); f.toggleClass('hidden');"}
      "+"]
    (add-transaction-form)
    (transaction-list
      (group-by :date transactions) 30)))
