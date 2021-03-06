(ns janus.json-response
  [:require [json-path]]
  [:use [clojure.data.json :only [read-json]]])

(defn extract-rule [clause]
  (nth clause 2))

(def type->pred { :string string?
                 :array sequential?
                 :object map?
                 :number number?})

(defmulti check (fn [rule & _]  rule))

(defmethod check :equal-to [_ expected actual]
  (if (not= actual expected)
    [(str "Expected \"" expected "\". Got \"" actual "\"")]
    []))

(defn check-children-dispatch [rule expected & _] [rule expected])
(defmulti check-children #'check-children-dispatch)



(defmethod check-children :default [rule expected children-clauses value]
  (if (empty? children-clauses)
    []
    [(str "Contract error: " rule " " expected " cannot specify child clauses: " children-clauses)]))

(defmethod check :of-type [_ expected actual]
  (let [pred (type->pred expected)]
    (cond 
     (nil? pred) [(str "unrecognised type " expected)]
     (not (pred actual)) [(str "Expected \"" actual "\" to be "  (name expected))]
     :else [])))

(defmethod check :matching [_ expected actual]
  (if (not (re-matches expected (str actual)))
    [(str "Expected \"" actual "\" to match regex " expected)]
    []))

(defn contextualize-failure [path failure]
  (str failure ", at path " path))

;; TODO: Avoid tuples, use maps
(defn verify-clause [json-doc [_ path rule expected children]]
  (let [doc-part  (json-path/at-path path json-doc)
        target (if (sequential? doc-part) doc-part [doc-part])
        verify (fn [value]
                 (let [failures (flatten [(check rule expected value) 
                                          (check-children rule expected children value)]
                                         )]
                   (map #(contextualize-failure path %) failures)))]
    (if (empty? target)
      [(str "Nothing found at path " path)]
      (mapcat #(verify %) target))))

(defmethod check-children [:of-type :object] [_ _ children-clauses value] 
  (map #(verify-clause value %) children-clauses))

(defn safe-read-json [doc]
  (try
    {:result :success :json-doc (read-json doc)}
    (catch Exception e
      {:result :failure :message (str "Invalid json document: " doc)})))

(defn verify-document [doc clauses]
  (let [{:keys [result message json-doc]} (safe-read-json doc)
        path-clauses (filter #(= :json-path (first %)) clauses)]
    (if (= :failure result)
      [message]
      (mapcat #(verify-clause json-doc %)
              path-clauses))))
