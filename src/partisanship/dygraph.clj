(ns partisanship.dygraph
  (:require (clojure.data [csv :as csv])
            (clojure.java [io :as io])
            (partisanship.data [vote :as vote])
            (partisanship [metrics :as metrics])
            (clj-time [format :as tf]
                      [core :as tc])))

(defn average [func votes]
  (let [contested (filter metrics/contested? votes)
        sum (reduce + (map func contested))
        tc (count contested)]
    (/ sum tc)))

(defn group-reduce-weighted [func current group]
  (reduce (fn [{:keys [score]} vote]
            {:date (:date vote)
             :score (+ (* 0.995 score) (* 0.005 (func vote)))})
          current group))

(defn weighted-averages [func votes]
  (let [groups (partition-by :date (filter metrics/contested? votes))
        start {:date nil :score (average func (apply concat (take 30 groups)))}]
    (next (reductions (partial group-reduce-weighted func) start groups))))

(defn dense-averages [func votes]
  (let [sparse-list (weighted-averages func votes)
        sparse-map (reduce (fn [sm val] (assoc sm (:date val) (:score val)))
                           (sorted-map)
                           sparse-list)
        start-date (tf/parse (first (keys sparse-map)))
        end-date (tf/parse (last (keys sparse-map)))
        date-range (map #(tf/unparse vote/date-fmt %)
                        (take-while #(not (tc/after? % end-date))
                                    (iterate #(tc/plus % (tc/days 1)) start-date)))]
    (map (fn [date]
           [date (format "%1$.2f"
                         (* 100 (second (first (rsubseq sparse-map <= date)))))])
         date-range)))

(defn dygraph-row [date & scores]
  (let [vals (concat (list date) (map str scores))]
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
