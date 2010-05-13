(ns org.scode.sob
  (:gen-class)
  (:require [org.scode.sob.markdown :as markdown]
            [clojure.contrib.command-line :as cmdline]
            [clojure.contrib.logging :as logging]
            [ring.adapter.jetty])
  (:use [org.scode.sob.repos :as repos]))

(defn die [msg]
  (logging/fatal msg)
  (System/exit 1))

(defn continuous-rescan
  [repos interval]
  (logging/info (str "starting continuous rescan at " interval " second intervals"))
  (loop []
    (Thread/sleep (* interval 1000))
    (try
     (dosync
      (alter repos repos/scan))
     (catch Throwable e
       (let [trace (java.io.StringWriter.)]
         (.printStackTrace e trace)
         (logging/error "repository rescan failed:\n" trace))))
    (recur)))

(defn start-continuous-rescan
  [repos interval]
  (.start (Thread. #(continuous-rescan repos interval))))

(defn make-blog-app
  [base path cont-rescan-interval]
  (let [repos (ref (repos/new path))]
    (when cont-rescan-interval
      (start-continuous-rescan repos cont-rescan-interval))
    (fn [req]
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (str req)})))

(defn serve-blog
  [port app]
  (ring.adapter.jetty/run-jetty app {:port port}))

(defn -main [& args]
  (cmdline/with-command-line args
    "sob - scode's own blog"
    [[port     "Select listen port" 8081]
     [base     "Select base URI path" "/"]
     ; todo: something's up with with-command-line and booleans.
     [continuous-rescan "Enable continuous rescan independent of requests" false]
     paths]
    (if (> 1 (count paths))
      (die "only one file system path currently supported"))
    (if (empty? paths)
      (die "must specify the path to a blog"))
    (logging/info (str "starting sob on *:" port base))
    (serve-blog port (make-blog-app base
                                    (first paths)
                                    (if continuous-rescan 1 nil)))))



