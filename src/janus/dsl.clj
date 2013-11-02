(ns janus.dsl)
(def should-have nil)
(defmulti should-have (fn [& args] (nth args 0)))
(defmethod should-have :path 
  ([_ path match expected]
     [:path path match expected])
  ([_ path match expected & children]
     [:path path match expected (vec children)]))

(defn of-type [type] [:of-type type])
(defn equal-to [expected] [:equal-to expected])

(defn url [path]
  [:property {:name "url" :value path}])

(defn method [method]
  [:method method])

(defn status [status]
  [:status status])

(defn serialization [s11n]
  [:property {:name "serialization" :value s11n}])


;; not a lot going on now...
(defn body [& clauses]
  [:body clauses])



(defn before [setup-func]
  [:property {:name "before" :value setup-func}])

(defn after [teardown-func]
  [:property {:name "after" :value teardown-func}])

(defn header [name value]
  [:header {:name name :value value}])

;; these actually dont do a hell of a lot
(def request  vector)
(def response vector)

(comment "OLD"
  [:contract {:name name
              :properties (map #(nth % 1) (filter #(= :property (first %)) definition))
              :clauses (map #(nth % 1) (filter #(= :clause (first %)) definition))
              :headers (map #(nth % 1) (filter #(= :header (first %)) definition))
              :body (first (map #(nth % 1) (filter #(= :body (first %)) definition)))}])

(defn contract 
  ([name endpoint request response] (assoc (contract name request response) :endpoint endpoint)) 
  
  ([name request response]
                     {:name name :request request :response response}))

(defn service [name & contracts]
  {:name name
   :contracts (vec contracts)})

(defn construct-domain [dsl-form]
  (let [dsl-ns (create-ns 'dsl-defn)
        compiled (binding [*ns* dsl-ns]
                   (eval '(clojure.core/refer 'clojure.core))
                   (eval '(refer 'janus.dsl))
                   (eval dsl-form))]
    (remove-ns 'dsl-defn)
    compiled))

