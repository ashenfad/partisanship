(ns partisanship.graph
  (:import (java.awt Color))
  (:require (partisanship.data [vote :as vote])
            (partisanship [metrics :as metrics])
            (clj-time [format :as tf])
            (incanter [core :as core]
                      [stats :as stats]
                      [charts :as charts])))

(defn average [func votes]
  (let [contested (filter metrics/contested? votes)
        sum (reduce + (map func contested))
        tc (count contested)]
    (/ sum tc)))

(defn yearly-averages [func votes]
  (let [groups (partition-by :year votes)
        years (map (comp :year first) groups)]
    (map (fn [y g] [y (average func g)]) years groups)))

(defn reduce-group [func current group]
  (reduce (fn [[_ avg] vote]
            [(:date vote)
             (+ (* 0.995 avg) (* 0.005 (func vote)))])
          current group))

(defn weighted-averages [func votes]
  (let [groups (partition-by :date (filter metrics/contested? votes))
        start [nil (average func (apply concat (take 30 groups)))]]
    (next (reductions (partial reduce-group func) start groups))))

(defn unity [votes & opts]
  (let [opt-map (apply hash-map opts)
        vals (for [p [:R :D]] (weighted-averages #(* 100 (metrics/unity p %)) votes))
        combined (apply map (fn [[date rv] [_ dv]]
                              [(.getMillis (tf/parse (str date))) rv dv]) vals)]
    (core/with-data (core/dataset [:time :r :d] combined)
      (doto (charts/time-series-plot :time :r)
        (charts/add-lines :time :d)
        (charts/set-alpha 0.6)
        (charts/set-y-label nil)
        (charts/set-x-label nil)
        (charts/set-title (:title opt-map))))))

(defn partisanship [votes & opts]
  (let [opt-map (apply hash-map opts)
        vals (map (fn [[y v]] [(.getMillis (tf/parse (str y))) v])
                  (weighted-averages #(* 100 (metrics/partisanship %)) votes))]
    (core/with-data (core/dataset [:time :v] vals)
      (doto (charts/time-series-plot :time :v)
        (charts/set-alpha 0.6)
        (charts/set-y-label nil)
        (charts/set-x-label nil)
        (charts/set-title (:title opt-map))))))

(defn generate-graphs []
  (core/save (partisanship (vote/house) :title "House - Partisanship")
             "graphs/house-partisanship.png" :width 1000 :height 300)
  (core/save (unity (vote/house) :title "House - Unity")
             "graphs/house-unity.png" :width 1000 :height 300)
  (core/save (partisanship (vote/senate) :title "Senate - Partisanship")
             "graphs/senate-partisanship.png" :width 1000 :height 300)
  (core/save (unity (vote/senate) :title "Senate - Unity")
             "graphs/senate-unity.png" :width 1000 :height 300))

(defn -main [& args]
  (println "Generating graphs...")
  (generate-graphs)
  (println "Finished!"))
