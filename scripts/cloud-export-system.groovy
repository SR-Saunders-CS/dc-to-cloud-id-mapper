// ============================================================
// SRM ID MAPPING TOOLKIT — CLOUD EXPORT (SYSTEM ENTITIES)
// Run in: ScriptRunner Cloud > Script Console
//
// WHAT THIS SCRIPT DOES:
//   Exports issue types, statuses, priorities, resolutions,
//   and projects from your Jira Cloud instance as a CSV.
//
//   This script is very fast — it makes only 5 API calls
//   regardless of instance size.
//
// THIS IS PART 2 OF 2:
//   Run cloud-export-fields.groovy first to export custom fields.
//   Then run this script to export system entities.
//   Paste both outputs into the CLOUD_CSV section of the
//   find-and-replace tool — this output goes directly below
//   the fields output (skip the header line of this output).
//
// HOW TO USE:
//   1. Run this script in ScriptRunner Cloud > Script Console
//   2. Click the LOGS tab (not the Result tab)
//   3. SCROLL TO THE VERY BOTTOM — the CSV is always last
//   4. You will see this line (ignore it, do not copy it):
//        >>>>>>>>>> CSV START <<<<<<<<<<
//   5. Start copying from the NEXT line — the one starting with:
//        "EntityType","Name","ParentName","Cloud_ID","Status"
//   6. Keep copying until the last Project line
//   7. Stop when you see (ignore it, do not copy it):
//        >>>>>>>>>> CSV END <<<<<<<<<<
//   8. Paste into CLOUD_CSV_SYSTEM in find-and-replace.groovy
// ============================================================

logger.warn('⏳ Running...')
logger.warn('When complete, scroll to the VERY BOTTOM of this Logs tab.')
logger.warn('Your CSV will be between the CSV START and CSV END markers.')
logger.warn('Copy everything between the markers — not the markers themselves.')

List<List<String>> rows = [['EntityType', 'Name', 'ParentName', 'Cloud_ID', 'Status']]

// Export issue types
def itResponse = get('/rest/api/3/issuetype')
    .header('Content-Type', 'application/json')
    .asObject(List)
assert itResponse.status == 200 : "Failed to fetch issue types: ${itResponse.status}"
(itResponse.body as List<Map>).each { it ->
    rows << (['IssueType', it['name'] as String, '',
              it['id'] as String, 'OK'] as List<String>)
}

// Export statuses
def statusResponse = get('/rest/api/3/status')
    .header('Content-Type', 'application/json')
    .asObject(List)
assert statusResponse.status == 200 : "Failed to fetch statuses: ${statusResponse.status}"
(statusResponse.body as List<Map>).each { s ->
    rows << (['Status', s['name'] as String, '',
              s['id'] as String, 'OK'] as List<String>)
}

// Export priorities
def priorityResponse = get('/rest/api/3/priority')
    .header('Content-Type', 'application/json')
    .asObject(List)
assert priorityResponse.status == 200 : "Failed to fetch priorities: ${priorityResponse.status}"
(priorityResponse.body as List<Map>).each { p ->
    rows << (['Priority', p['name'] as String, '',
              p['id'] as String, 'OK'] as List<String>)
}

// Export resolutions
def resolutionResponse = get('/rest/api/3/resolution')
    .header('Content-Type', 'application/json')
    .asObject(List)
assert resolutionResponse.status == 200 : "Failed to fetch resolutions: ${resolutionResponse.status}"
(resolutionResponse.body as List<Map>).each { r ->
    rows << (['Resolution', r['name'] as String, '',
              r['id'] as String, 'OK'] as List<String>)
}

// Export projects (ParentName column holds the project key)
def projectResponse = get('/rest/api/3/project')
    .header('Content-Type', 'application/json')
    .asObject(List)
assert projectResponse.status == 200 : "Failed to fetch projects: ${projectResponse.status}"
(projectResponse.body as List<Map>).each { p ->
    rows << (['Project', p['name'] as String, p['key'] as String,
              p['id'] as String, 'OK'] as List<String>)
}

logger.warn("Exported: ${rows.size() - 1} system entities (issue types, statuses, priorities, resolutions, projects)")

def csv = rows.collect { row ->
    row.collect { cell ->
        "\"${(cell ?: '').replace('"', '""')}\""
    }.join(',')
}.join('\n')

logger.warn('>>>>>>>>>> CSV START <<<<<<<<<<')
logger.warn("\n${csv}\n")
logger.warn('>>>>>>>>>> CSV END <<<<<<<<<<')
