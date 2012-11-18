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

(defn character-class [c] [{:operator :character-class :domain c}])

(defn c_plus [c] [{:operator :quantification :minimum 1}
                  [(character-class c)]])

;; Basic non-regex goals

(fact "A" (logic/run 1 [q] (charactero [\A] q ) ) => '(\A))
(fact "no class" (logic/run 1 [q] (charactero [] q)) => '())
(fact "backwards" (first (logic/run 3 [q] (charactero q \A))) => [\A])

;; min 1 is the root, one child of a character of class c
(fact "A"
  (logic/run 1 [q] (character-classo (character-class [\A]) q) ) => [\A])
(comment
  (fact "A+"
    (->> (logic/run 20 [q] (regex-matcho ) )
         (map #(apply str %))
         (map #(re-matches #"A+" %))
         (filter nil?))
    => [])


  (fact "B+"
    (->> (logic/run 20 [q] (regex-matcho q (c_plus [\C])))
         (map #(apply str %))
         (map #(re-matches #"B+" %))
         (filter nil?))
    => []))