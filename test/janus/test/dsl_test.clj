(ns janus.test.dsl-test
  [:use [janus.dsl]]
  [:require [midje.sweet :as midje]])

(midje/unfinished)

(midje/fact "should-have creates clauses as required"
            (should-have :path "path" :of-type :type) => [:path "path" :of-type :type]
            (should-have :path "foo" :of-type :object 
                         (should-have :path "bar" :matching ...amazing-regex...)) => 
                         [:path "foo" :of-type :object 
                          [[:path "bar" :matching ...amazing-regex...]]])

(midje/fact "url creates a property containing the path"
            (url "path") => [:property {:name "url" :value "path"}])

(midje/fact "method creates a property containing the method"
            (method :meth) => [:method :meth])

(midje/fact "status creates a property containing the status"
            (status 105) => [:status 105])

;; overloading 'body' in req and resp... will be dodgy 
(midje/fact "body creates a body object with a type and data or matching clauses"
            (body (of-type :json) (equal-to {:sample "obj"}))
            => [:body [[:of-type :json] [:equal-to {:sample "obj"}]]]
            (body (should-have :path "foo" :equal-to "foo")) 
            => [:body [[:path "foo" :equal-to "foo"]]])


(midje/fact "before creates a property containing the setup function"
            (before 'setup-func) => [:property {:name "before" :value 'setup-func}])

(midje/fact "after creates a property containing the teardown function"
            (after 'teardown-func) => [:property {:name "after" :value 'teardown-func}])

(midje/fact "header creates a header"
            (header "Name" "Value") => [:header {:name "Name" :value "Value"}])

(midje/fact "request creates a request with matchers inside"
            (request (header "Name" "Value")) => [[:header {:name "Name" :value "Value"}]])

(midje/fact "respons creates a response with matchers inside"
            (response (header "Name" "Value")) => [[:header {:name "Name" :value "Value"}]])


(defn contract-with [check-key expected-value]
  (midje/chatty-checker [actual-contract]
                  (= expected-value (check-key (nth actual-contract 1)))))


(midje/fact "defining a contract as a request and response pairing"
            (contract "sample" (request) (response)) => {:name "sample" :request [] :response []})

(midje/fact "defining a contract with an endpoint"
            (contract "sample" "/uri" (request) (response)) => {:name "sample" :endpoint "/uri" :request [] :response []})

(midje/fact "defining a service as a cluster of contracts "
            (service "sample" 
                     (contract "first contract" "/start" (request) (response))
                     (contract "second contract" (request) (response))) => 
                     { :name "sample" 
                      :contracts [{:endpoint "/start", 
                                   :name "first contract", 
                                   :request [], :response []} 
                                  {:name "second contract", :request [], :response []}]} )

(midje/fact "loading a DSL program"
            (construct-domain '(service "sample")) => {:name "sample", :contracts []})


