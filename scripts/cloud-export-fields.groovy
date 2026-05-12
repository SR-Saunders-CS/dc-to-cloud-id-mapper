// ============================================================
// SRM ID MAPPING TOOLKIT — CLOUD EXPORT (FIELDS)
// Run in: ScriptRunner Cloud > Script Console
//
// WHAT THIS SCRIPT DOES:
//   Exports all custom fields and their options from your
//   Jira Cloud instance as a CSV.
//
//   This script is optimised for large instances — it only
//   fetches options for fields that actually support them
//   (select lists, multi-selects, radio buttons, checkboxes,
//   cascading selects). All other field types are exported
//   without making unnecessary API calls.
//
// THIS IS PART 1 OF 2:
//   Run this script first to export custom fields.
//   Then run cloud-export-system.groovy to export issue types,
//   statuses, priorities, resolutions, and projects.
//   Paste both outputs into the CLOUD_CSV section of the
//   find-and-replace tool.
//
// HOW TO USE:
//   1. Run this script in ScriptRunner Cloud > Script Console
//   2. Click the LOGS tab (not the Result tab)
//   3. SCROLL TO THE VERY BOTTOM — the CSV is always last
//   4. You will see this line (ignore it, do not copy it):
//        >>>>>>>>>> CSV START <<<<<<<<<<
//   5. Start copying from the NEXT line — the one starting with:
//        "EntityType","Name","ParentName","Cloud_ID","Status"
//   6. Keep copying until the last CustomFieldOption line
//   7. Stop when you see (ignore it, do not copy it):
//        >>>>>>>>>> CSV END <<<<<<<<<<
//   8. Paste into CLOUD_CSV_FIELDS in find-and-replace.groovy
// ============================================================

logger.warn('⏳ Running — this may take a minute on large instances.')
logger.warn('When complete, scroll to the VERY BOTTOM of this Logs tab.')
logger.warn('Your CSV will be between the CSV START and CSV END markers.')
logger.warn('Copy everything between the markers — not the markers themselves.')

// Field types that support options — only these get options fetched
Set<String> OPTION_FIELD_TYPES = [
    'com.atlassian.jira.plugin.system.customfieldtypes:select',
    'com.atlassian.jira.plugin.system.customfieldtypes:multiselect',
    'com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons',
    'com.atlassian.jira.plugin.system.customfieldtypes:multicheckboxes',
    'com.atlassian.jira.plugin.system.customfieldtypes:cascadingselect'
] as Set<String>

List<List<String>> rows = [['EntityType', 'Name', 'ParentName', 'Cloud_ID', 'Status']]
def seenOptionIds = [] as Set<String>

// Fetch all fields and filter to custom fields only
def response = get('/rest/api/3/field')
    .header('Content-Type', 'application/json')
    .asObject(List)
assert response.status == 200 : "Failed to fetch fields: ${response.status}"

def allFields    = response.body as List<Map>
def customFields = allFields.findAll { it['custom'] == true }

// Detect duplicate field names
def fieldNameCounts = [:] as Map<String, Integer>
customFields.each { field ->
    String name = field['name'] as String
    fieldNameCounts[name] = (fieldNameCounts[name] ?: 0) + 1
}

int fieldCount   = 0
int skippedCount = 0

customFields.each { field ->
    String fieldId   = field['id'] as String
    String fieldName = field['name'] as String
    String status    = fieldNameCounts[fieldName] > 1 ? 'AMBIGUOUS' : 'OK'
    rows << (['CustomField', fieldName, '', fieldId, status] as List<String>)
    fieldCount++

    // Check if this field type supports options
    Map schema      = field['schema'] as Map
    String typeKey  = schema ? schema['custom'] as String : null

    if (!typeKey || !OPTION_FIELD_TYPES.contains(typeKey)) {
        skippedCount++
        return // skip options fetch for non-option fields
    }

    // Fetch contexts for this field
    def ctxResponse = get("/rest/api/3/field/${fieldId}/context")
        .header('Content-Type', 'application/json')
        .asObject(Map)
    if (ctxResponse.status != 200) return
    def contexts = (ctxResponse.body as Map)['values'] as List<Map>
    if (!contexts) return

    contexts.each { ctx ->
        String ctxId = ctx['id'] as String

        // Fetch options with pagination
        int startAt      = 0
        int maxResults   = 100
        boolean isLast   = false

        while (!isLast) {
            def optResponse = get("/rest/api/3/field/${fieldId}/context/${ctxId}/option")
                .header('Content-Type', 'application/json')
                .queryString('startAt', startAt.toString())
                .queryString('maxResults', maxResults.toString())
                .asObject(Map)
            if (optResponse.status != 200) break

            Map optBody      = optResponse.body as Map
            List<Map> options = optBody['values'] as List<Map>
            isLast           = optBody['isLast'] as Boolean ?: true
            if (!options) break

            def parentOptions = options.findAll { !(it['optionId']) }
            def childOptions  = options.findAll {   it['optionId']  }

            def optNameCounts = [:] as Map<String, Integer>
            parentOptions.each { opt ->
                String v = opt['value'] as String
                optNameCounts[v] = (optNameCounts[v] ?: 0) + 1
            }

            parentOptions.each { opt ->
                String optId  = opt['id'] as String
                String optVal = opt['value'] as String
                if (!seenOptionIds.add(optId)) return
                String optStatus = optNameCounts[optVal] > 1 ? 'AMBIGUOUS' : 'OK'
                rows << (['CustomFieldOption', optVal, fieldName,
                          optId, optStatus] as List<String>)

                childOptions.findAll { it['optionId'] == optId }.each { child ->
                    String childId  = child['id'] as String
                    String childVal = child['value'] as String
                    if (!seenOptionIds.add(childId)) return
                    rows << (['CustomFieldOption', childVal,
                              "${fieldName} > ${optVal}".toString(),
                              childId, 'OK'] as List<String>)
                }
            }

            startAt += options.size()
            if (options.size() < maxResults) isLast = true
        }
    }
}

logger.warn("Exported ${fieldCount} custom fields. Skipped options fetch for ${skippedCount} non-option fields.")

def csv = rows.collect { row ->
    row.collect { cell ->
        "\"${(cell ?: '').replace('"', '""')}\""
    }.join(',')
}.join('\n')

logger.warn('>>>>>>>>>> CSV START <<<<<<<<<<')
logger.warn("\n${csv}\n")
logger.warn('>>>>>>>>>> CSV END <<<<<<<<<<')
