(ns janus.json-response
  [:require [json-path]]
  [:use [clojure.data.json :only [read-json]]])

(defn extract-rule [clause]
  (nth clause 2))

(defn equal-to [expected actual]
  (if (not= actual expected)
    [(str "Expected \"" expected "\". Got \"" actual "\"")]
    []))

(def type->pred { :string string?
                 :array sequential?
                 :object map?
                 :number number?})

(defn of-type [expected actual]
  (let [pred (type->pred expected)]
    (cond 
     (nil? pred) [(str "unrecognised type " expected)]
     (not (pred actual)) [(str "Expected \"" actual "\" to be "  (name expected))]
     :else [])))

(defn matching [expected actual]
  (if (not (re-find expected (str actual)))
    [(str "Expected \"" actual "\" to match regex " expected)]
    []))

(defn check [rule expected actual]
  (cond
   (= :equal-to rule) (equal-to expected actual)
   (= :of-type rule) (of-type expected actual)
   (= :matching rule) (matching expected actual)
   :else [(str "Unknown matcher " rule)]))


(defn contextualize-failure [path failure]
  (str failure ", at path " path))

;; todo, simplify by returning empty seq for success

(defn verify-clause [json-doc [_ path rule expected & children]]
  (let [doc-part  (json-path/at-path path json-doc)
        target (if (sequential? doc-part) doc-part [doc-part])
        verify (fn [value]
                 (let [failure (check rule expected value)]
                   (map #(contextualize-failure path %) failure)))]
    (if (empty? target)
      [(str "Nothing found at path " path)]
      (mapcat #(verify %) target))))

(defn verify-document [doc clauses]
  (let [json-doc (read-json doc)]
    (mapcat #(verify-clause json-doc %)
         clauses)))
