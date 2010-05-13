(ns #^{:author "peter.schuller@infidyne.com"
       :doc "Blog post repository handling.

A blog post repository on disk is simply a directory containing files
ending in '.post'. This module helps to scan this on-disk structure and responde to changes incrementally."}
  org.scode.sob.repos
  (:require [org.scode.sob.markdown :as markdown]
            [clojure.contrib.duck-streams :as duck-streams]
            [clojure.contrib.logging :as logging]))

(defn- scan)

(defn new
 "Make a new repository at the given path. The path need not exist,
  can be empty, or can contain one or more posts.

  A repos has at least these keys:

    :path - the path given to this function
    :pages - a set of pages
 "
 [path]
 ; Perform an immediate scan because it tends to be helpful to see
 ; failures immediately in case there are any; both during development
 ; and during deployment.
 (scan {:path path
        :last-scan 0
        :scan-interval 1
        :scan-checkpoint 0
        :pages #{}}))

(defn- now [] (System/currentTimeMillis))

(defn- read-from-string
  "Use the clojure reader to read the string, and return the result."
  [s]
  (read (java.io.PushbackReader. (java.io.StringReader. s))))

(defn- make-page
  [f]
  (logging/info (str "processing new or possibly changed post " f))
  (let [source (duck-streams/slurp* f)]
    {:fname (.getName f)
     :scan-checkpoint (dec (System/currentTimeMillis))
     :source source
     }));; todo, process/render

(defn- scan-pages
  "Inspect path for new, removed or updated pages relative to old-pages,
   and return a new pages map."
  [dir old-pages]
  (let [now (System/currentTimeMillis)
        file-map (into {} (for [file (.listFiles dir)
                                :when (.endsWith (.getName file) ".post")]
                            [(.getName file) file]))
        old-map (zipmap (map :fname old-pages) old-pages)
        make-page-cached (fn [[fname f]]
                           (let [old (get old-map fname)]
                             (if (and old
                                      (< (.lastModified f) (:scan-checkpoint old)))
                               old
                               (make-page f))))]
    (into #{} (map make-page-cached file-map))))

(defn scan
  "Re-scan disk for new/removed/changed data and return an updated repos."
  [repos]
  (let [dir (java.io.File. (:path repos))]
    (when (not (.exists dir))
      (throw (Exception. (str dir " does not exist"))))
    (let [last-modified (.lastModified dir)
          now (now)]
      (if (> last-modified (:scan-checkpoint repos))
        (do
          (logging/info (str "repos changed: " (:path repos)))
          (conj repos
                [:pages (scan-pages dir (:pages repos))]
                [:scan-checkpoint (dec now)]))
        repos))))

(defn maybe-scan
  [repos]
  "Return (scan repos) if the scanning interval has been exceeded, else
   return repos."
  (if (> (- (now) (:last-scan repos)) (:scan-interval repos))
    (scan repos)
    repos))
