(ns janus.test.regex-unify-test
  [:require [clojure.core.logic :as logic]]
  [:use [janus.regex-unify]
   [midje.sweet]])

(fact
  (make-char-set \a \b) => [\a \b])

(facts
  (logic/run 1 [q] (charo q \a true)) => '(\a)
  (logic/run 1 [q] (charo q \a false)) => '((_.0 :- (!= _.0 \a))))

(facts
  (logic/run 1 [q] (containso q '(:a :b) true)) => '(:a)
  (logic/run 1 [q] (containso q '(:a :b) false)) => '(_.0))

(fact
  (count (first (logic/run 1 [q] (matching-stringo q [\a \b] 2)))) => 2)

(facts
  (count (logic/run 1 [q] (repeato 1 10 [\a \b] q true))) => 1)


;; todo; better asserts...test generative?



(defn basic-regex [operator params] {:operator operator :params params})






(defn with-children [regex children]
  (update-in regex [ :params :children] (fnil #(concat children %) [])))

(defn character-class [c] (basic-regex :character-class {:domain c}))

(defn c-plus [c] (-> (basic-regex :quantification {:minimum 1})
                     (with-children [ (character-class c)])))

(defn regex-sequence [& children]
  (-> (basic-regex :sequence nil)
      (with-children children)) )

(defn options [& children]
  (-> (basic-regex :option nil)
      (with-children children)))


(defn run-logic [logic-fn regex]
(let [results (logic/run 20 [q] (logic-fn q))]
(->> results
     (map #(apply str %))
     (map #(re-matches regex %))
     (filter nil?))))

(defn find-regex-matches [regex results]
  (->> results
       (map flatten)
       (map #(apply str %))
       (map #(re-matches regex %))))

;; basically put it into the weird format my logic program spitting out
(defn logicify-string [s]
  (->> (seq s)
       (map vector)
       (vec)))


;; Basic non-regex goal
(fact "no class" (logic/run 1 [q] (charactero [] q)) => '())
(fact "backwards"
  (logic/lfirst (first (logic/run 1 [q] (charactero q \A))))
  => \A)
(fact "A"
  (logic/run 20 [q] (charactero  [\A] q))
  => [\A])

(fact "[AB]"
  (logic/run 20 [q] (charactero [\A \B] q)) => [\A \B])

;; regexes

(fact "A+"
  (->> (logic/run 10 [q] (regex-matcho (c-plus [\A]) q))
       (find-regex-matches #"A+")) => (has-prefix ["A" "AA" "AAA"]))


(fact "[BA]+"
  (->> (logic/run 10 [q] (regex-matcho (c-plus [\B \A]) q))
       (find-regex-matches #"[BA]+")) => (has not-any? nil?))

;; sequence of character classes
(fact "[12][AB]"
  (->> (logic/run 10 [q] (regex-matcho
                          (regex-sequence
                           (character-class [\1 \2])
                           (character-class [\A \B]))
                          q))
       (find-regex-matches #"[12][AB]")) => (has-prefix ["1A" "2A" "1B" "2B"]))

;; Sequence of character classes and quantifications

(fact "[1234]+A[XYZ]"
  (->> (logic/run 10 [q] (regex-matcho
                          (regex-sequence
                           (c-plus [\1 \2 \3 \4])
                           (character-class [\A])
                           (character-class [\X \Y \Z]))
                          q))
       (find-regex-matches #"[1234]+A[XYZ]")) =>
       (has-prefix ["1AX" "1AY" "1AZ" "2AX" "2AY"]))

;;options


(fact "a|b|c"
  (->> (logic/run 10 [q] (regex-matcho
                          (options
                           (character-class [\A])
                           (character-class [\B])
                           (character-class [\C]))
                          q))
       (find-regex-matches #"A|B|C")) =>
       [ "A" "B" "C"])

;; use it backwards


;; don't care too much about this one i think
(fact "Backwards: AB"
  (first (logic/run 1 [q] (regex-matcho q (logicify-string "AB")))) => (contains {:operator :quantification}))