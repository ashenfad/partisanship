(ns partisanship.data.vote
  (:require (clojure.data [json :as json])
            (clojure.java [io :as io])
            (clj-time [format :as tf])))

(def data-dir "data/")
(def files {:house "house-votes.data" :senate "senate-votes.data"})
(def date-fmt (:year-month-day tf/formatters))

(defn- get-file
  "Returns the data file for the congressional branch (:senate or :house)"
  [branch-type]
  (str data-dir (branch-type files)))

(defn branch [branch-type]
  "Returns a vector of the roll-call votes given either :house or :senate."
  (try (with-open [rdr (clojure.java.io/reader (get-file branch-type))]
         (vec (map json/read-json (line-seq rdr))))
       (catch Exception _ nil)))

(defn house []
  "Returns a vector of the roll-call votes for the house."
  (branch :house))

(defn senate []
  "Returns a vector of the roll-call votes for the house."
  (branch :senate))

(defn- scrape-year [scraper year & [vote _]]
  (let [offset (if vote #(+ vote %) inc)
        vote-range (map offset (range))]
    (remove empty? (take-while identity (map #(scraper year %) vote-range)))))

(defn- scrape-range [votes]
  (let [{:keys [year vote]} (last votes)
        first-year (if year year 1990)
        first-vote (if vote (inc vote) 1)
        rest-years (next (iterate inc first-year))
        rest-scrape (map (fn[x] [x 1]) rest-years)]
    (concat [[first-year first-vote]] rest-scrape)))

(defn- new-votes [scraper votes]
  (reduce concat (take-while (comp not empty?)
                             (map #(apply (partial scrape-year scraper) %)
                                  (scrape-range votes)))))

(defn update
  "Updates the roll-call votes for the congressional branch (:senate or :house)
   using the 'scraper' function"
  [scraper branch-type]
  (let [current-votes (new-votes scraper (branch branch-type))]
    (with-open [wrtr (io/writer (get-file branch-type) :append true)]
      (doseq [line (map json/json-str current-votes)]
        (.write wrtr (str line "\n"))))))
