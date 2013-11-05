(service "simple JSON service"
      (contract "GET document"
        (method :get)
        (url "http://localhost:4568/service")
        (header "Content-Type" "application/json")

        (matching-jsonpath "$.id" :of-type :number)
        (matching-jsonpath "$.features[*]" :matching #"[a-z]")))

