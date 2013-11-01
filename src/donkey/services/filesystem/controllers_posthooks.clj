(ns donkey.services.filesystem.controllers-posthooks
  (:use [donkey.services.filesystem.controllers])
  (:require [dire.core :refer [with-post-hook!]]
            [clojure.tools.logging :as log]))

(defn- log-func
  [func-name]
  )


(with-post-hook! #'do-move (log-func "do-move"))
(with-post-hook! #'do-create (log-func "do-create"))
(with-post-hook! #'do-metadata-get (log-func "do-metadata-get"))
(with-post-hook! #'do-metadata-set (log-func "do-metadata-set"))
(with-post-hook! #'do-share (log-func "do-share"))
(with-post-hook! #'do-unshare (log-func "do-unshare"))
(with-post-hook! #'do-metadata-batch-set (log-func "do-metadata-batch-set"))
(with-post-hook! #'do-metadata-delete (log-func "do-metadata-delete"))
(with-post-hook! #'do-preview (log-func "do-preview"))
(with-post-hook! #'do-exists (log-func "do-exists"))
(with-post-hook! #'do-stat (log-func "do-stat"))
(with-post-hook! #'do-manifest (log-func "do-manifest"))
(with-post-hook! #'do-download (log-func "do-download"))
(with-post-hook! #'do-special-download (log-func "do-special-download"))
(with-post-hook! #'do-user-permissions (log-func "do-user-permissions"))
(with-post-hook! #'do-copy (log-func "do-copy"))
(with-post-hook! #'do-groups (log-func "do-groups"))
(with-post-hook! #'do-quota (log-func "do-quota"))
(with-post-hook! #'do-add-tickets (log-func "do-add-tickets"))
(with-post-hook! #'do-remove-tickets (log-func "do-remove-tickets"))
(with-post-hook! #'do-list-tickets (log-func "do-list-tickets"))
(with-post-hook! #'do-paths-contain-space (log-func "do-paths-contain-space"))
(with-post-hook! #'do-replace-spaces (log-func "do-replace-spaces"))
(with-post-hook! #'do-read-chunk (log-func "do-read-chunk"))
(with-post-hook! #'do-overwrite-chunk (log-func "do-overwrite-chunk"))
(with-post-hook! #'do-get-csv-page (log-func "do-get-csv-page"))
(with-post-hook! #'do-read-csv-chunk (log-func "do-read-csv-chunk"))
(with-post-hook! #'do-upload (log-func "do-upload"))
