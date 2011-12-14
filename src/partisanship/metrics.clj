(ns partisanship.metrics
  (:import (java.lang Math))
  (:require (partisanship.data [vote :as vote])))

(defn approval
  "Returns the approval ratio of the vote, either overall or for the given
   party."
  ([vote]
     (let [votes (vals (select-keys (:outcome vote) [:D :R]))
           yeas (reduce + 0 (remove nil? (map :yea votes)))
           total (double (reduce + 0 (flatten (map vals votes))))]
       (when (pos? total)
         (/ yeas total))))
  ([party vote]
     (let [outcome (party (:outcome vote))
           total (double (reduce + 0 (vals outcome)))]
       (when (pos? total)
         (/ (:yea outcome 0) total)))))

(defn partisanship
  "The partisanship score for a vote.  1 indicates maximum partisanship
   (Reps and Dems voting completely opposite).  0 indicates no difference
   in voting behavior between parties."
  [vote]
  (let [dem-approval (approval :D vote)
        rep-approval (approval :R vote)]
    (when (and dem-approval rep-approval)
      (Math/abs (- dem-approval rep-approval)))))

(defn- contest [vote-approval]
  (when vote-approval
    (* 2 (Math/abs (- 0.5 vote-approval)))))

(defn unity
  "The overall unity for a vote. 1 indicates complete agreement and 0
   indicates total disagreement (50/50 split). If a party is given, the
   unity score represents party unity on the vote."
  ([vote]
     (contest (approval vote)))
  ([party vote]
     (contest (approval party vote))))

(defn contested?
  "Returns true if more than 10% of the votes are against the majority
   opinion."
  [vote]
  (when-let [score (unity vote)]
    (> 0.80 score)))
