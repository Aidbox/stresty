(ns auth
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [b64]))

(defn- get-admin-header [ctx]
  {"Authorization"
   (str "Basic "
        (b64/encode
          (str (:client-id ctx) ":" (:client-secret ctx))))})

(defn- auth-user-req [ctx]
  {:url              (str (:base-url ctx) "/auth/token")
   :method           :post
   :throw-exceptions false
   :headers          {"content-type" "application/json"}
   :body             (json/generate-string {:username      (:auth-user ctx)
                                            :password      (:auth-user-password ctx)
                                            :client_id     (:auth-client-id ctx)
                                            :client_secret (:auth-client-secret ctx)
                                            :grant_type    "password"})})

(defn get-user-header [ctx]
  (let [{body :body :as resp} (-> ctx
                                  auth-user-req
                                  http/request)
        json-resp             (json/parse-string body true)
        access-token          (:access_token json-resp)]
    {"Authorization" (str "Bearer " access-token)}))

(defn- get-auth-headers [ctx]
  (let [headers {:admin (get-admin-header ctx)}]
    (cond-> headers
      (every? ctx [:auth-client-id :auth-client-secret :auth-user :auth-user-password])
      (assoc :user (get-user-header ctx)))))

(defn add-auth-headers [ctx]
  (merge ctx (auth/get-auth-headers ctx)))

(comment

  (get-auth-headers {:base-url      "http://access-policy-box.aidbox.io"
                     :client-id     "postman"
                     :client-secret "postman"})

  (get-auth-headers {:base-url           "http://access-policy-box.aidbox.io"
                     :client-id          "postman"
                     :client-secret      "postman"
                     :auth-client-id     "myapp"
                     :auth-client-secret "verysecret"
                     :auth-user          "patient-user"
                     :auth-user-password "admin"})

  )


