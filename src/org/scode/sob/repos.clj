(ns #^{:author "peter.schuller@infidyne.com"
       :doc "Blog post repository handling.

A blog post repository on disk is simply a directory containing files
ending in '.post'. This module helps to scan this on-disk structure and responde to changes incrementally."}
  org.scode.sob.repos
  (:require [org.scode.sob.markdown :as markdown]
            [clojure.contrib.logging :as logging]))

(defn- scan)

(defn new
 "Make a new repository at the given path. The path need not exist,
  can be empty, or can contain one or more posts.

  The returned repository is intended to be opaque."
 [path]
 ; Perform an immediate scan because it tends to be helpful to see
 ; failures immediately in case there are any; both during development
 ; and during deployment.
 (scan {:path path
        :last-scan 0
        :scan-interval 1
        :scan-checkpoint 0
        :pages {}}))

(defn- now [] (System/currentTimeMillis))

(defn- read-from-string
  "Use the clojure reader to read the string, and return the result."
  [s]
  (read (java.io.PushbackReader. (java.io.StringReader. s))))

(defn- scan-pages
  "Inspect path for new, removed or updated pages relative to old-pages,
   and return a new pages map."
  [dir old-pages]
  {});todo

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
