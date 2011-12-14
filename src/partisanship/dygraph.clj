(ns partisanship.dygraph
  (:require (clojure.data [csv :as csv])
            (clojure.java [io :as io])
            (partisanship.data [vote :as vote])
            (partisanship [metrics :as metrics])
            (clj-time [format :as tf]
                      [core :as tc])))

(defn fraction-sum [func group]
  (let [score (int (* 1000 (reduce + (map func group))))
        score-count (* 10 (count group))
        date (:date (first group))]
    {:date date :score score :score-count score-count}))

(defn sparse-averages [func votes]
  (let [groups (partition-by :date (filter metrics/contested? votes))]
    (map #(fraction-sum func %) groups)))

(defn dense-averages [func votes]
  (let [sparse-list (sparse-averages func votes)
        sparse-map (reduce (fn [sm val] (assoc sm (:date val) (dissoc val :date)))
                           (sorted-map)
                           sparse-list)
        start-date (tf/parse (first (keys sparse-map)))
        end-date (tf/parse (last (keys sparse-map)))
        date-range (map #(tf/unparse vote/date-fmt %)
                        (take-while #(not (tc/after? % end-date))
                                    (iterate #(tc/plus % (tc/days 1)) start-date)))]
    (map (fn [date] [date (get sparse-map date)]) date-range)))

(defn dygraph-score [score]
  (if score
    (str (:score score) "/" (:score-count score))
    "null"))

(defn dygraph-row [date & scores]
  (let [vals (concat (list date) (map dygraph-score scores))]
    (apply str (interpose "," vals))))

(defn write-file [file rows]
  (with-open [wrtr (io/writer file)]
    (doseq [line rows]
      (.write wrtr (str line "\n")))))

(defn generate-partisanship [branch-type]
  (let [averages (dense-averages metrics/partisanship (vote/branch branch-type))
        averages (remove #(nil? (second %)) averages)
        rows (map #(apply dygraph-row %) averages)]
    (write-file (str "data/dygraph-partisanship-" (name branch-type) ".csv") rows)))
