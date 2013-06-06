# Table of Contents

* [Application Metadata Endpoints](#application-metadata-endpoints)
    * [Listing Workflow Elements](#listing-workflow-elements)
    * [Searching for Deployed Components](#searching-for-deployed-components)
    * [Listing Analysis Identifiers](#listing-analysis-identifiers)
    * [Deleting Categories](#deleting-categories)
    * [Valiating Analyses for Pipelines](#valiating-analyses-for-pipelines)
    * [Listing Data Objects in an Analysis](#listing-data-objects-in-an-analysis)
    * [Categorizing Analyses](#categorizing-analyses)
    * [Listing Analysis Categorizations](#listing-analysis-categorizations)
    * [Determining if an Analysis Can be Exported](#determining-if-an-analysis-can-be-exported)
    * [Adding Analyses to Analysis Groups](#adding-analyses-to-analysis-groups)
    * [Getting Analyses in the JSON Format Required by the DE](#getting-analyses-in-the-json-format-required-by-the-de)
    * [Getting Analysis Details](#getting-analysis-details)
    * [Listing Analysis Groups](#listing-analysis-groups)
    * [Listing Individual Analyses](#listing-individual-analyses)
    * [Exporting a Template](#exporting-a-template)
    * [Exporting an Analysis](#exporting-an-analysis)
    * [Exporting Selected Deployed Components](#exporting-selected-deployed-components)
    * [Permanently Deleting an Analysis](#permanently-deleting-an-analysis)
    * [Logically Deleting an Analysis](#logically-deleting-an-analysis)
    * [Previewing Templates](#previewing-templates)
    * [Previewing Analyses](#previewing-analyses)
    * [Updating an Existing Template](#updating-an-existing-template)
    * [Updating an Analysis](#updating-an-analysis)
    * [Forcing an Analysis to be Updated](#forcing-an-analysis-to-be-updated)
    * [Updating App Labels](#updating-app-labels)
    * [Importing a Template](#importing-a-template)
    * [Importing an Analysis](#importing-an-analysis)
    * [Importing Deployed Components](#importing-deployed-components)
    * [Updating Top-Level Analysis Information](#updating-top-level-analysis-information)
    * [Getting Analyses in the JSON Format Required by the DE](#getting-analyses-in-the-json-format-required-by-the-de)
    * [Rating Analyses](#rating-analyses)
    * [Deleting Analysis Ratings](#deleting-analysis-ratings)
    * [Searching for Analyses](#searching-for-analyses)
    * [Listing Analyses in an Analysis Group](#listing-analyses-in-an-analysis-group)
    * [Listing Analyses that may be Included in a Pipeline](#listing-analyses-that-may-be-included-in-a-pipeline)
    * [Listing Deployed Components in an Analysis](#listing-deployed-components-in-an-analysis)
    * [Updating the Favorite Analyses List](#updating-the-favorite-analyses-list)
    * [Making an Analysis Available for Editing in Tito](#making-an-analysis-available-for-editing-in-tito)
    * [Making a Copy of an Analysis Available for Editing in Tito](#making-a-copy-of-an-analysis-available-for-editing-in-tito)
    * [Submitting an Analysis for Public Use](#submitting-an-analysis-for-public-use)
    * [Making a Pipeline Available for Editing](#making-a-pipeline-available-for-editing)
    * [Making a Copy of a Pipeline Available for Editing](#making-a-copy-of-a-pipeline-available-for-editing)
    * [Requesting Installation of a Tool](#requesting-installation-of-a-tool)
    * [Updating a Tool Installation Request (User)](#updating-a-tool-installation-request-(user))
    * [Updating a Tool Installation Request (Administrator)](#updating-a-tool-installation-request-(administrator))
    * [Listing Tool Installation Requests](#listing-tool-installation-requests)
    * [Listing Tool Installation Request Details](#listing-tool-installation-request-details)

# Application Metadata Endpoints

## Listing Workflow Elements

Unsecured Endpoint: GET /get-workflow-elements/{element-type}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Searching for Deployed Components

Unsecured Endpoint: GET /search-deployed-components/{search-term}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Listing Analysis Identifiers

Unsecured Endpoint: GET /get-all-analysis-ids

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Deleting Categories

Unsecured Endpoint: POST /delete-categories

This endpoint is a passthrough to the metactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Valiating Analyses for Pipelines

Unsecured Endpoint: GET /validate-analysis-for-pipelines/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Listing Data Objects in an Analysis

Unsecured Endpoint: GET /analysis-data-objects/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Categorizing Analyses

Unsecured Endpoint: POST /categorize-analyses

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Listing Analysis Categorizations

Unsecured Endpoint: GET /get-analysis-categories/{category-set}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Determining if an Analysis Can be Exported

Unsecured Endpoint: POST /can-export-analysis

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Adding Analyses to Analysis Groups

Unsecured Endpoint: POST /add-analysis-to-group

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Getting Analyses in the JSON Format Required by the DE

Unsecured Endpoint: GET /get-analysis/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Getting Analysis Details

Unsecured Endpoint: GET /analysis-details/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Listing Analysis Groups

Secured Endpoint: GET /secured/app-groups

Unsecured Endpoint: GET /get-only-analysis-groups/{workspace-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
paths. Please see the metadactyl documentation for more information.

## Listing Individual Analyses

Unsecured Endpoint: GET /list-analysis/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Exporting a Template

Unsecured Endpoint: GET /export-template/{template-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Exporting an Analysis

Unsecured Endpoint: GET /export-workflow/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Exporting Selected Deployed Components

Unsecured Endpoint: POST /export-deployed-components

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Permanently Deleting an Analysis

Unsecured Endpoint: POST /permanently-delete-workflow

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Logically Deleting an Analysis

Unsecured Endpoint: POST /delete-workflow

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Previewing Templates

Unsecured Endpoint: POST /preview-template

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Previewing Analyses

Unsecured Endpoint: POST /preview-workflow

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Updating an Existing Template

Unsecured Endpoint: POST /update-template

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Updating an Analysis

Unsecured Endpoint: POST /update-workflow

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Forcing an Analysis to be Updated

Unsecured Endpoint: POST /force-update-workflow

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Updating App Labels

Unsecured Endpoint: POST /update-app-labels

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Importing a Template

Unsecured Endpoint: POST /import-template

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Importing an Analysis

Unsecured Endpoint: POST /import-workflow

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Importing Deployed Components

Unsecured Endpoint: POST /import-tools

This service is an extension of the /import-workflow endpoint that also sends a
notification for every deployed component that is imported provided that a
username and e-mail address is provided for the notification. The request body
should be in the following format:

```json
{
    "components": [
        {
            "name": "component-name",
            "location": "component-location",
            "implementation": {
                "implementor_email": "e-mail-address-of-implementor",
                "implementor": "name-of-implementor",
                "test": {
                    "params": [
                        "param-1",
                        "param-2",
                        "param-n"
                    ],
                    "input_files": [
                        "input-file-1",
                        "input-file-2",
                        "input-file-n"
                    ],
                    "output_files": [
                        "output-file-1",
                        "output-file-2",
                        "output-file-n"
                    ]
                }
            },
            "type": "deployed-component-type",
            "description": "deployed-component-description",
            "version": "deployed-component-version",
            "attribution": "deployed-component-attribution",
            "user": "username-for-notification",
            "email": "e-mail-address-for-notification"
        }
    ]
}
```

Note that this format is identical to the one used by the /import-workflow
endpoint except that the `user` and `email` fields have been added to allow
notifications to be generated automatically. If either of these fields is
missing or empty, a notification will not be sent even if the deployed component
is imported successfully.

The response body for this service contains a success flag along with a brief
description of the reason for the failure if the deployed components can't be
imported.

Here's an example of a successful import:

```
$ curl -sd '
{
    "components": [
        {
            "name": "foo",
            "location": "/usr/local/bin",
            "implementation": {
                "implementor_email": "nobody@iplantcollaborative.org",
                "implementor": "Nobody",
                "test": {
                    "params": [],
                    "input_files": [],
                    "output_files": []
                }
            },
            "type": "executable",
            "description": "the foo is in the bar",
            "version": "1.2.3",
            "attribution": "the foo needs no attribution",
            "user": "nobody",
            "email": "nobody@iplantcollaborative.org"
        }
    ]
}
' http://by-tor:8888/import-tools | python -mjson.tool
{
    "success": true
}
```

Here's an example of an unsuccessful import:

```
$ curl -sd '
{
    "components": [
        {
            "name": "foo",
            "location": "/usr/local/bin",
            "implementation": {
                "implementor_email": "nobody@iplantcollaborative.org",
                "implementor": "Nobody"
            },
            "type": "executable",
            "description": "the foo is in the bar",
            "version": "1.2.3",
            "attribution": "the foo needs no attribution",
            "user": "nobody",
            "email": "nobody@iplantcollaborative.org"
        }
    ]
}
' http://by-tor:8888/import-tools | python -mjson.tool
{
    "reason": "org.json.JSONException: JSONObject[\"test\"] not found.",
    "success": false
}
```

Though it is possible to import analyses using this endpoint, this practice is
not recommended because it can cause spurious notifications to be sent.

## Updating Top-Level Analysis Information

Unsecured Endpoint: POST /update-analysis

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Getting Analyses in the JSON Format Required by the DE

Secured Endpoint: GET /secured/template/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Rating Analyses

Secured Endpoint: POST /secured/rate-analysis

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Deleting Analysis Ratings

Secured Endpoint: POST /secured/delete-rating

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Searching for Analyses

Secured Endpoint: GET /secured/search-analyses

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Listing Analyses in an Analysis Group

Secured Endpoint: GET /secured/get-analyses-in-group/{group-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Listing Analyses that may be Included in a Pipeline

Secured Endpoint: GET /secured/list-analyses-for-pipeline/{group-id}

This service is an alias for the `/get-analyses-in-group/{group-id}` service.
At one time, this was a different service that returned additional information
that was normally omitted for the sake of efficiency. Some recent efficiency
improvements have eliminated the need to omit this information from the more
commonly used endpoint, however. This endpoint is currently being retained for
backward compatibility.

## Listing Deployed Components in an Analysis

Secured Endpoint: GET /secured/get-components-in-analysis/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Updating the Favorite Analyses List

Secured Endpoint: POST /secured/update-favorites

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Making an Analysis Available for Editing in Tito

Secured Endpoint: GET /secured/edit-template/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Making a Copy of an Analysis Available for Editing in Tito

Secured Endpoint: GET /secured/copy-template/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Submitting an Analysis for Public Use

Secured Endpoint: POST /secured/make-analysis-public

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Making a Pipeline Available for Editing

Secured Endpoint: GET /secured/edit-workflow/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Making a Copy of a Pipeline Available for Editing

Secured Endpoint: GET /secured/copy-workflow/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Requesting Installation of a Tool

Secured Endpoint: PUT /secured/tool-request

This service is primarily a passthrough to the metadactyl endpoint using the
same path. The only difference is that this endpoint also sends a message to the
tool request email address and generates a notification for the new tool request
indicating that the tool request was successfully submitted. Please see the
metadactyl documentation for more details.

## Updating a Tool Installation Request (User)

Secured Endpoint: POST /secured/tool-request

This service is a passthrough to the metadactyl endpoint using the same
path. Please se the metadactyl documentation for more details.

## Updating a Tool Installation Request (Administrator)

Unsecured Endpoint: POST /tool-request

This service is primarily a passthrough to the metadactyl endpoint using the
same path. The only difference is that this endpoint also generates a
notification for the tool request status update. Please see the metadactyl
documentation for more details.

## Listing Tool Installation Requests

Secured Endpoint: GET /secured/tool-requests

This service is a passthrough to the metadactyl endpoint using the same
path. Please se the metadactyl documentation for more details.

## Listing Tool Installation Request Details

Unsecured Endpoint: GET /tool-request/{tool-request-id}

This service is a passthrough to the metadactyl endpoint using the same
path. Please se the metadactyl documentation for more details.