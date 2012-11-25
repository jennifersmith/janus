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

(defn with-children [regex & children]
  (update-in regex [ :params :children] (fnil #(concat children %) [])))

(defn character-class [c] (basic-regex :character-class {:domain c}))

(defn c-plus [c] (-> (basic-regex :quantification {:minimum 1})
                     (with-children (character-class c))))



;; Basic non-regex goal
(fact "no class" (logic/run 1 [q] (charactero [] q)) => '())
(fact "backwards"
  (logic/lfirst (first (logic/run 1 [q] (charactero q \A))))
  => \A)

(fact "A"
  (logic/run 20 [q] (charactero  [\A] q))
  => [\A])

(fact "[AB]"
  (logic/run 20 [q]
             (charactero [\A \B] q))
     => [\A \B])

(defn run-logic [logic-fn regex]
(let [results (logic/run 20 [q] (logic-fn q))]
(->> results
     (map #(apply str %))
     (map #(re-matches regex %))
     (filter nil?))))

(defn find-regex-matches [regex results]
  (->> results
       (map #(apply str %))
       (map #(re-matches regex))))

(fact "A+"
  (->> (logic/run 10 [q] (regex-matcho (c-plus [\A]) q))
       (find-regex-matches #"A+")) => (has-prefix ["A" "AA" "AAA"]))
