;; In this tutorial we analyse the data of the
;; [Clojure Calendar Feed](https://clojureverse.org/t/the-clojure-events-calendar-feed-turns-2/).

;; ## Setup

(ns index
  (:require [scicloj.kindly.v4.kind :as kind]
            [clojure.string :as str]
            [tablecloth.api :as tc]
            [scicloj.tableplot.v1.plotly :as plotly]
            [tech.v3.datatype.datetime :as datetime]))

(defonce feed-string
  (slurp "https://www.clojurians-zulip.org/feeds/events.ics"))

;; ## Initial exploration

(kind/hiccup
 [:div {:style {:max-height "400px"
                :overflow-y :auto}}
  (kind/code
   feed-string)])

;; ## Data parsing

(def feed-dataset
  (-> feed-string
      (str/split #"END:VEVENT\nBEGIN:VEVENT")
      (->> (map (fn [event-string]
                  (-> event-string
                      str/split-lines
                      (->> (map (fn [event-line]
                                  (when-let [k (re-find #"URL:|SUMMARY:|DTSTART:" event-line)]
                                    [(-> k
                                         (str/replace ":" "")
                                         str/lower-case
                                         keyword)
                                     (-> event-line
                                         (str/replace k ""))])))
                           (into {}))))))
      (tc/dataset {:parser-fn {:dtstart [:local-date-time
                                         "yyyyMMdd'T'HHmmss'Z'"]}})
      (tc/set-dataset-name "Clojure Calendar Feed")))

;; ## Exploring the dataset

feed-dataset

(type feed-dataset)

(map? feed-dataset)

(keys feed-dataset)

(:dtstart feed-dataset)

(type (:dtstart feed-dataset))

;; ## Visualizing progress

;; ## Ordering

(-> feed-dataset
    (tc/order-by [:dtstart]))

;; ## Adding a count column

(-> feed-dataset
    (tc/order-by [:dtstart])
    (tc/add-column :count
                   (fn [ds]
                     (range (tc/row-count ds)))))

;; ## Plotting the time-series

(-> feed-dataset
    (tc/order-by [:dtstart])
    (tc/add-column :count
                   (fn [ds]
                     (range (tc/row-count ds))))
    (plotly/layer-point {:=x :dtstart
                         :=y :count}))


;; ## Adding the year column

(-> feed-dataset
    (tc/order-by [:dtstart])
    (tc/add-column :count
                   (fn [ds]
                     (range (tc/row-count ds))))
    (tc/add-column :year
                   (fn [ds]
                     (datetime/long-temporal-field
                      :years
                      (:dtstart ds)))))

;; ## Filtering by year -- keeping the last couple of years

(-> feed-dataset
    (tc/order-by [:dtstart])
    (tc/add-column :count
                   (fn [ds]
                     (range (tc/row-count ds))))
    (tc/add-column :year
                   (fn [ds]
                     (datetime/long-temporal-field
                      :years
                      (:dtstart ds))))
    (tc/select-rows (fn [row]
                      (-> row
                          :year
                          (>= 2023)))))

;; ## Visualize again


(-> feed-dataset
    (tc/order-by [:dtstart])
    (tc/add-column :count
                   (fn [ds]
                     (range (tc/row-count ds))))
    (tc/add-column :year
                   (fn [ds]
                     (datetime/long-temporal-field
                      :years
                      (:dtstart ds))))
    (tc/select-rows (fn [row]
                      (-> row
                          :year
                          (>= 2023))))
    (plotly/layer-point {:=x :dtstart
                         :=y :count}))


;; ## Making the plot informative

(-> feed-dataset
    (tc/order-by [:dtstart])
    (tc/add-column :count
                   (fn [ds]
                     (range (tc/row-count ds))))
    (tc/add-column :year
                   (fn [ds]
                     (datetime/long-temporal-field
                      :years
                      (:dtstart ds))))
    (tc/select-rows (fn [row]
                      (-> row
                          :year
                          (>= 2023))))
    (plotly/layer-point {:=x :dtstart
                         :=y :count
                         :=text :summary}))


;; ## Recognizing groups

(def groups-pattern
  #"london-clojurians|los-angeles-clojure|visual-tools|data-recur|real-world-data|scicloj-llm|scicloj-ai|macroexpand|clojure-dsp")

(-> feed-dataset
    (tc/order-by [:dtstart])
    (tc/add-column :count
                   (fn [ds]
                     (range (tc/row-count ds))))
    (tc/add-column :year
                   (fn [ds]
                     (datetime/long-temporal-field
                      :years
                      (:dtstart ds))))
    (tc/select-rows (fn [row]
                      (-> row
                          :year
                          (>= 2023))))
    (tc/select-rows :url)
    (tc/map-columns :group
                    [:url]
                    (fn [url]
                      (re-find groups-pattern (str/lower-case url)))))

;; ## Colouring by group

(-> feed-dataset
    (tc/order-by [:dtstart])
    (tc/add-column :count
                   (fn [ds]
                     (range (tc/row-count ds))))
    (tc/add-column :year
                   (fn [ds]
                     (datetime/long-temporal-field
                      :years
                      (:dtstart ds))))
    (tc/select-rows (fn [row]
                      (-> row
                          :year
                          (>= 2023))))
    (tc/select-rows :url)
    (tc/map-columns :group
                    [:url]
                    (fn [url]
                      (re-find groups-pattern
                               (str/lower-case url))))
    (plotly/layer-point {:=x :dtstart
                         :=y :count
                         :=color :group}))


;; ## Time series per group

(-> feed-dataset
    (tc/order-by [:dtstart])
    (tc/add-column :year
                   (fn [ds]
                     (datetime/long-temporal-field
                      :years
                      (:dtstart ds))))
    (tc/select-rows (fn [row]
                      (-> row
                          :year
                          (>= 2023))))
    (tc/select-rows :url)
    (tc/map-columns :group
                    [:url]
                    (fn [url]
                      (re-find groups-pattern
                               (str/lower-case url))))
    (tc/group-by [:group])
    (tc/add-column :count
                   (fn [ds]
                     (range (tc/row-count ds))))
    tc/ungroup)

;; ## Plotting the multiple time series

(-> feed-dataset
    (tc/order-by [:dtstart])
    (tc/add-column :year
                   (fn [ds]
                     (datetime/long-temporal-field
                      :years
                      (:dtstart ds))))
    (tc/select-rows (fn [row]
                      (-> row
                          :year
                          (>= 2023))))
    (tc/select-rows :url)
    (tc/map-columns :group
                    [:url]
                    (fn [url]
                      (re-find groups-pattern
                               (str/lower-case url))))
    (tc/group-by [:group])
    (tc/add-column :count
                   (fn [ds]
                     (range (tc/row-count ds))))
    tc/ungroup
    (plotly/base {:=x :dtstart
                  :=y :count
                  :=color :group
                  :=text :summary})
    plotly/layer-point 
    plotly/layer-line)




