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
                   ((emptyo r) (== c match))
                   ((== c match))
                   ((charactero r match)))))))

(defn character-classo [regex match]
  (fresh [character-domain, root]
         (firsto regex root)
         (== root (partial-map {:domain character-domain}))
         (charactero character-domain match)))

(declare regex-matcho)
(defn quantificationo [regex match]
                 (fresh [minimum, root, children, child]
                        (conso root children regex)
                        (== root (partial-map {:minimum minimum}))
                        (firsto children child)
                        (== minimum 1)
                        (fresh [head tail]
                               (conso head tail match)
                               (regex-matcho child head))))

(defn pluso [q inner]
(logic/all
 (logic/fresh [h]
        (logic/firsto q h)
        (inner h))
   (logic/fresh [r]
          (logic/resto q r)
          (logic/conde
           ((logic/== r '()))
           ((pluso r inner))))))

(defn regex-matcho [regex match]
  (fresh [pm operator root children]
         (firsto regex root)
         (resto regex children )
         (== pm (partial-map {:operator operator}))
         (== pm root)
         (matche [operator]
                 [[:quantification] (quantificationo regex match)]
                 [[:character-class] (character-classo regex match)])) )


(defn partial-maps-dont-unify-like-i-like [q]
  (fresh [m1 m2]
         (== m1 (partial-map {:foo 1}))
         (== m2 (partial-map {:baro 1}))
         (== q m1)
         ))