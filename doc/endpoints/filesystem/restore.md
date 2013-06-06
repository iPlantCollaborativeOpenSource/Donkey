Restoring a file or directory from a user's trash
-------------------------------------------------

__URL Path__: /secured/filesystem/restore

__HTTP Method__: POST

__Error Codes__: ERR_EXISTS, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER, ERR_NOT_WRITEABLE

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "paths" : ["/iplant/trash/home/proxy-user/johnworth/foo.fq",
                   "/iplant/trash/home/proxy-user/johnworth/foo1.fq"]
    }

__Response Body__:

    {
        "status" : "success",
        "restored" : {
            "/iplant/trash/home/proxy-user/johnworth/foo.fq" :  {
                "restored-path" : /iplant/home/johnworth/foo.fq",
                "partial-restore" : true
            },
            "/iplant/trash/home/proxy-user/johnworth/foo1.fq" : {
                "restored-path" : "/iplant/home/johnworth/foo1.fq"
                "partial-restore" : true
            }
        }
    }

The "restored" field is a map that whose keys are the paths in the trash that were restored. Associated with those paths is a map that contains two entries, "restored-path" that contains the path that the file was restored to, and "partial-restore" which is a boolean that is true if the restoration was to the home directory because there was no alternative and false if the restoration was a full restore.

__Curl Command__:

    curl -d '{"paths" : ["/iplant/trash/home/proxy-user/johnworth/foo.fq", "/iplant/trash/home/proxy-user/johnworth/foo1.fq"]}' http://sample.nibblonian.org/secured/filesystem/restore?proxyToken=notReal



