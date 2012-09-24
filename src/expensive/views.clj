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

(defn transaction-view [t]
  [:div.transaction-view
    {:id        (str (t :_id))
     :direction (t :direction)}
    [:div.category (t :category)]
    [:div.amount (format "%.2f" (float (t :amount)))]
    [:div.source (t :source)]])

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
                  (assoc-in
                    (assoc-in (transaction-view t)
                      [1 :onclick] (str "location.href='/transactions/" (str (t :_id)) "';"))
                    [1 :alternate] (= (mod i 2) 1)))
                ts)]])))])

(defn n-day-date-selector [start-date n selected]
  (let [date-str-selected (date-for-db selected)]
    [:select {:name "date"}
      (for [i (range (inc n))
            :let [date      (datetime/plus start-date (datetime/days i))
                  date-str  (date-for-db date)
                  selected? (= date-str-selected date-str)]]
        [:option {:value date-str :selected selected?}
          (date-for-humans date)])]))

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
        (n-day-date-selector
          (datetime/minus (datetime/now) (datetime/days 30)) 60 (datetime/now))]
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

(defn edit-transaction-form [t]
  [:div.edit-transaction-form
    [:form {:action "" :method "POST"}
      [:div.field
        [:input#category-field {:type "text" :name "category" :value (t :category)}]]
      [:div.field
        [:input {:type "text" :name "amount" :value (t :amount)}]]
      [:div#source-field.field
        [:select {:name "source"}
          [:option "Source"]
          [:option {:value "cash"    :selected (= "cash"    (t :source))} "Cash"]
          [:option {:value "bank"    :selected (= "bank"    (t :source))} "Bank"]
          [:option {:value "savings" :selected (= "savings" (t :source))} "Savings"]]]
      [:div#direction-field.field
        [:select {:name "direction"}
          [:option "Direction"]
          [:option {:value "in"  :selected (= "in"  (t :direction))} "In"]
          [:option {:value "out" :selected (= "out" (t :direction))} "Out"]]]
      [:div#date-field.field
        (n-day-date-selector
          (datetime/minus (datetime/now) (datetime/days 30)) 60
          (date-from-db-format (t :date)))]
      [:div.button
        [:input.button {:type "submit" :value "Update"}]]]])

(defn transaction [t]
  (layout
    (transaction-view t)
    (edit-transaction-form t)))
