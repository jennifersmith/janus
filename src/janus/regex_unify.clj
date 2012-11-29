(ns janus.regex-unify
    (:refer-clojure :exclude [==])
    (:use clojure.core.logic)
    (:require [clojure.core.logic :as logic]))

(defn make-char-set [start end]
  (vec (map char (range (int start) (inc (int end))))))

(defn charo [q chr matched]
  (logic/conde [(logic/== q chr)
                (logic/== matched true)]
               [(logic/!= q chr)
                (logic/== matched false)]))

(defn containso [q sequence found]
  (logic/conde [(logic/== true (empty? sequence))
                (logic/== false found)]
               [(logic/!= nil (first sequence))
                (logic/== q (first sequence))
                (logic/== true found)]
               [(if (empty? sequence)
                  logic/fail
                  (containso q (rest sequence) found))]))

(defn matching-stringo [q chr-set length]
  (logic/== q (apply str (take length (repeatedly #(nth chr-set (rand-int (count chr-set))))))))


(defn repeato [min max char-set q matched]
  (letfn [(next [length]
            (logic/conde [(logic/== true matched)
                          (matching-stringo q char-set length)]
                         [(logic/== false matched)
                          (logic/== (apply str (repeat length "b")) q)]
                         [(logic/!= length max)
                          (next (inc length))]))]
    (next min)))


;; A+

(defn charactero [character-domain match]
  (conde
   ((emptyo character-domain) fail)
   ((fresh [c,r]
           (conso c r character-domain)
           (conde
            ((== c match))
            ((!= r '()) (charactero r match)))))))

(defn character-classo [regex-params match]
  (fresh [character-domain, root]
         (== regex-params (partial-map {:domain character-domain}))
         (fresh [h r]
                (conso h r match)
                (emptyo r)
                (charactero character-domain h))))

(declare regex-matcho
)
(defn pluso [inner match]
(logic/all
 (logic/fresh [h]
        (logic/firsto match h)
        (regex-matcho inner h))
   (logic/fresh [r]
          (logic/resto match r)
          (logic/conde
           ((logic/== r '()))
           ((pluso inner r))))))

(defn quantificationo [regex-params match]
                 (fresh [minimum, children, child]
                        (== regex-params
                            (partial-map
                             {:minimum minimum :children children}))
                        (== minimum 1)
                        (firsto children child)
                        (resto children [])
                        (pluso child match)))


(defn regex-matcho [regex match]
  (fresh [pm operator params]
         (== pm
             (partial-map
              {:operator operator
               :params params}))
         (== regex pm)
         (matche [operator]
                 [[:quantification] (quantificationo params match)]
                 [[:character-class] (character-classo params match)])))
