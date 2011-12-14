(ns partisanship.data.scrape-senate
  (:require (partisanship.data [vote :as vote])
            (clojure [string :as string])
            (clj-time [format :as tf])
            (net.cgrand [enlive-html :as html])))

(def in-fmt (tf/formatter "MMMM dd, yyyy,  HH:mm a"))

(defn- year-to-session [year]
  {:congress (int (/ (- year 1787) 2))
   :session (if (odd? year) 1 2)})

(defn- to-url [year vote]
  (let [{:keys [congress session]} (year-to-session year)]
    (str "http://www.senate.gov/legislative/LIS/roll_call_votes/vote" congress
         session "/vote_" congress "_" session "_" (format "%05d" vote) ".xml")))

(defn- pattern [res pattern]
  (map html/text (html/select res pattern)))

(defn- scraper [year vote]
  (try
    (let [res (html/html-resource (java.net.URL. (to-url year vote)))
          votes (map (fn [p v] {(keyword p) {(keyword (string/lower-case v)) 1}})
                     (pattern res [:party])
                     (pattern res [:vote_cast]))
          outcome (reduce (partial merge-with (partial merge-with +)) votes)
          outcome (apply merge
                         (map (fn [[p o]] {p (select-keys o [:yea :nay])}) outcome))
          date (first (pattern res [:vote_date]))]
      (when date
        (println "  Scraping" year vote)
        (if (not (or (empty? outcome) (empty? (:D outcome)) (empty? (:R outcome))))
          {:year year
           :vote vote
           :date (tf/unparse vote/date-fmt (tf/parse in-fmt date))
           :outcome outcome}
          {})))
    (catch Exception _ nil)))

(defn -main [& args]
  (println "Update senate roll-call votes.  This might take a while...")
  (vote/update scraper :senate)
  (println "Finished!"))
