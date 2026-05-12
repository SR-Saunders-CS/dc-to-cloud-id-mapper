// ============================================================
// SRM ID MAPPING TOOLKIT — FIND & REPLACE TOOL
// Run in: ScriptRunner Cloud > Script Console
//
// WHAT THIS SCRIPT DOES:
//   Scans a ScriptRunner script for hardcoded DC IDs,
//   shows you exactly what changed between DC and Cloud,
//   and gives you an updated script with all IDs replaced.
//
//   You must test the updated script before using it in
//   production. This tool guides you — it does not guarantee
//   correctness.
//
// ⚠️  IMPORTANT — ALWAYS TEST YOUR UPDATED SCRIPT
//   This tool makes its best attempt at replacing IDs correctly.
//   Some replacements are guesses based on context.
//   You must test every updated script before using it in production.
// ============================================================


// ============================================================
// YOUR INPUTS — THIS IS THE ONLY SECTION YOU NEED TO EDIT
// ============================================================

// MODE — what do you want to do?
//   'FIX_SCRIPT'   — scan SCRIPT_TO_FIX and replace IDs (default)
//   'SHOW_MAPPING' — print the full DC → Cloud ID reference table
String MODE = 'FIX_SCRIPT'


// ─────────────────────────────────────────────────────────────
// INPUT 1 OF 3 — YOUR DC EXPORT
// ─────────────────────────────────────────────────────────────
// 1. Run dc-export.groovy on your DC instance
// 2. Click the Logs tab and scroll to the very bottom
// 3. Copy everything between the markers (not the markers themselves)
// 4. Delete ONLY the placeholder line below — keep the ''' lines
// 5. Paste your output where the placeholder was
// ─────────────────────────────────────────────────────────────
String DC_CSV = '''
PASTE YOUR DC EXPORT HERE — DELETE THIS LINE ONLY, KEEP THE TRIPLE QUOTES ABOVE AND BELOW
'''


// ─────────────────────────────────────────────────────────────
// INPUT 2 OF 3 — YOUR CLOUD FIELDS EXPORT
// ─────────────────────────────────────────────────────────────
// 1. Run cloud-export-fields.groovy on your Cloud instance
// 2. Click the Logs tab and scroll to the very bottom
// 3. Copy everything between the markers (not the markers themselves)
// 4. Delete ONLY the placeholder line below — keep the ''' lines
// 5. Paste your output where the placeholder was
// ─────────────────────────────────────────────────────────────
String CLOUD_CSV_FIELDS = '''
PASTE YOUR CLOUD FIELDS EXPORT HERE — DELETE THIS LINE ONLY, KEEP THE TRIPLE QUOTES ABOVE AND BELOW
'''


// ─────────────────────────────────────────────────────────────
// INPUT 3 OF 3 — YOUR CLOUD SYSTEM EXPORT
// ─────────────────────────────────────────────────────────────
// 1. Run cloud-export-system.groovy on your Cloud instance
// 2. Click the Logs tab and scroll to the very bottom
// 3. Copy everything between the markers (not the markers themselves)
// 4. Delete ONLY the placeholder line below — keep the ''' lines
// 5. Paste your output where the placeholder was
// ─────────────────────────────────────────────────────────────
String CLOUD_CSV_SYSTEM = '''
PASTE YOUR CLOUD SYSTEM EXPORT HERE — DELETE THIS LINE ONLY, KEEP THE TRIPLE QUOTES ABOVE AND BELOW
'''


// ─────────────────────────────────────────────────────────────
// INPUT — THE SCRIPT YOU WANT TO FIX
// ─────────────────────────────────────────────────────────────
// 1. Find the ScriptRunner script you want to fix
//    (e.g. ScriptRunner → Listeners, Post Functions, Jobs,
//    Escalation Services — open the script, copy the entire body)
// 2. Delete ONLY the placeholder line below — keep the ''' lines
// 3. Paste your script where the placeholder was
// ─────────────────────────────────────────────────────────────
String SCRIPT_TO_FIX = '''
PASTE YOUR SCRIPT HERE — DELETE THIS LINE ONLY, KEEP THE TRIPLE QUOTES ABOVE AND BELOW
'''


// ============================================================
// STOP HERE — do not edit anything below this line
// ============================================================

import java.util.regex.Pattern
import java.util.regex.Matcher


// ============================================================
// SECTION 2 — DATA CLASS
// ============================================================

class MappingEntry {
    String entityType
    String name
    String parent
    String dcId
    String cloudId
    String status

    MappingEntry(String entityType, String name, String parent,
                 String dcId, String cloudId, String status) {
        this.entityType = entityType
        this.name       = name
        this.parent     = parent
        this.dcId       = dcId
        this.cloudId    = cloudId
        this.status     = status
    }
}


// ============================================================
// SECTION 3 — CSV PARSING
// ============================================================

// Check for unfilled placeholders
if (DC_CSV.trim() == 'PASTE YOUR DC EXPORT HERE') {
    logger.warn('ERROR: You have not pasted your DC export into DC_CSV.\nGo back to INPUT 1 OF 3 and follow the instructions.')
    return
}
if (CLOUD_CSV_FIELDS.trim() == 'PASTE YOUR CLOUD FIELDS EXPORT HERE') {
    logger.warn('ERROR: You have not pasted your Cloud fields export into CLOUD_CSV_FIELDS.\nGo back to INPUT 2 OF 3 and follow the instructions.')
    return
}
if (CLOUD_CSV_SYSTEM.trim() == 'PASTE YOUR CLOUD SYSTEM EXPORT HERE') {
    logger.warn('ERROR: You have not pasted your Cloud system export into CLOUD_CSV_SYSTEM.\nGo back to INPUT 3 OF 3 and follow the instructions.')
    return
}
if (SCRIPT_TO_FIX.trim() == 'PASTE YOUR SCRIPT HERE' && MODE == 'FIX_SCRIPT') {
    logger.warn('ERROR: You have not pasted a script into SCRIPT_TO_FIX.\nGo back to INPUT — THE SCRIPT YOU WANT TO FIX and follow the instructions.')
    return
}

// Combine both cloud exports into one string for parsing
String CLOUD_CSV = CLOUD_CSV_FIELDS.trim() + '\n' + CLOUD_CSV_SYSTEM.trim()

Pattern csvFieldPattern = Pattern.compile('"((?:[^"]|"")*)"')

List<List<String>> dcRows    = []
List<List<String>> cloudRows = []

DC_CSV.trim().split('\n').each { String line ->
    String trimmedLine = line.trim()
    if (!trimmedLine) return
    List<String> fields = []
    Matcher m = csvFieldPattern.matcher(trimmedLine)
    while (m.find()) {
        String val = m.group(1)
        if (val != null) fields.add(val.replace('""', '"'))
    }
    if (fields) dcRows.add(fields)
}

CLOUD_CSV.trim().split('\n').each { String line ->
    String trimmedLine = line.trim()
    if (!trimmedLine) return
    List<String> fields = []
    Matcher m = csvFieldPattern.matcher(trimmedLine)
    while (m.find()) {
        String val = m.group(1)
        if (val != null) fields.add(val.replace('""', '"'))
    }
    // Skip header rows — handles multiple CSV outputs pasted together
    if (fields && fields[0] == 'EntityType') return
    if (fields) cloudRows.add(fields)
}


// ============================================================
// SECTION 4 — BUILD THE MAPPING
// ============================================================

if (dcRows.size() < 2) {
    logger.warn('ERROR: DC CSV data is empty or could not be parsed.\nMake sure you pasted the full output from dc-export.groovy into DC_CSV.')
    return
}
if (cloudRows.size() < 2) {
    logger.warn('ERROR: Cloud CSV data is empty or could not be parsed.\nMake sure you pasted the output from cloud-export-fields.groovy into CLOUD_CSV_FIELDS\nand the output from cloud-export-system.groovy into CLOUD_CSV_SYSTEM.')
    return
}

Map<String, String>  cloudIdByKey  = new LinkedHashMap<String, String>()
Map<String, Integer> cloudKeyCount = new LinkedHashMap<String, Integer>()

cloudRows.eachWithIndex { List<String> row, int rowIndex ->
    if (rowIndex == 0) return
    if (row.size() < 4) return
    String joinKey = "${row[0]}|${row[1]}|${row[2]}".toString()
    if (!cloudIdByKey.containsKey(joinKey)) {
        cloudIdByKey.put(joinKey, row[3])
    }
    cloudKeyCount.put(joinKey, (cloudKeyCount.get(joinKey) ?: 0) + 1)
}

List<MappingEntry> idMapping = []

dcRows.eachWithIndex { List<String> row, int rowIndex ->
    if (rowIndex == 0) return
    if (row.size() < 4) return
    String entityType = row[0]
    String name       = row[1]
    String parent     = row[2]
    String dcId       = row[3]
    String dcStatus   = row.size() > 4 ? row[4] : 'OK'
    String joinKey    = "${entityType}|${name}|${parent}".toString()
    String cloudId    = cloudIdByKey.get(joinKey) ?: 'UNRESOLVED'
    String status
    if (dcStatus == 'AMBIGUOUS') {
        status = 'AMBIGUOUS'
    } else if (cloudId == 'UNRESOLVED') {
        status = 'UNRESOLVED'
    } else if ((cloudKeyCount.get(joinKey) ?: 0) > 1) {
        status = 'AMBIGUOUS'
    } else {
        status = 'OK'
    }
    idMapping.add(new MappingEntry(entityType, name, parent, dcId, cloudId, status))
}

if (!idMapping) {
    logger.warn('ERROR: Mapping is empty after joining the two CSVs.\nCheck that both CSV sections have data rows below the header.')
    return
}


// ============================================================
// SECTION 5 — FULL MAPPING MODE
// ============================================================

if (MODE == 'SHOW_MAPPING') {
    String W = ('═' * 64).toString()
    String N = ('─' * 64).toString()

    StringBuilder out = new StringBuilder()
    out.append('\n').append(W).append('\n')
    out.append('  SRM ID MAPPING TOOLKIT — FULL MAPPING REFERENCE\n')
    out.append("  ${idMapping.size()} entries built from your DC and Cloud exports\n".toString())
    out.append(W).append('\n')

    ['CustomField', 'CustomFieldOption', 'IssueType',
     'Status', 'Priority', 'Resolution', 'Project'].each { String et ->
        List<MappingEntry> group = idMapping.findAll { MappingEntry e ->
            e.entityType == et
        }
        if (!group) return
        out.append("\n  ${et.toUpperCase()} (${group.size()})\n".toString())
        out.append(N).append('\n')
        out.append("  ${'DC ID'.padRight(26)}${'Cloud ID'.padRight(26)}${'Name'.padRight(32)}Parent / Key\n".toString())
        out.append(N).append('\n')
        group.each { MappingEntry e ->
            String dcCol    = e.dcId.padRight(26)
            String cloudCol = (e.cloudId == 'UNRESOLVED'
                ? 'UNRESOLVED ⚠'.padRight(26)
                : e.cloudId.padRight(26)).toString()
            String nameCol  = e.name.padRight(32)
            String flag     = e.status == 'UNRESOLVED' ? '  ← not migrated'
                            : e.status == 'AMBIGUOUS'  ? '  ← duplicate name ⚠'
                            : ''
            out.append("  ${dcCol}${cloudCol}${nameCol}${e.parent}${flag}\n".toString())
        }
    }
    out.append('\n').append(W).append('\n')
    logger.warn(out.toString())
    return
}


// ============================================================
// SECTION 6 — SCAN THE SCRIPT FOR IDs
// ============================================================

// Check for DC Groovy Behaviour scripts
boolean isBehaviourScript = SCRIPT_TO_FIX.contains('FieldBehaviours') ||
                            SCRIPT_TO_FIX.contains('@BaseScript')

// Find all customfield_XXXXX patterns
Pattern CUSTOM_FIELD_PATTERN = Pattern.compile('customfield_(\\d+)')
Matcher customFieldMatcher   = CUSTOM_FIELD_PATTERN.matcher(SCRIPT_TO_FIX)
Set<String> customFieldIdsFound = new LinkedHashSet<String>()
while (customFieldMatcher.find()) {
    customFieldIdsFound.add(customFieldMatcher.group(0))
}

// Find all bare numeric IDs >= 10000
Pattern BARE_NUMBER_PATTERN = Pattern.compile('\\b(\\d+)\\b')
Matcher bareNumberMatcher   = BARE_NUMBER_PATTERN.matcher(SCRIPT_TO_FIX)
Set<String> bareNumericIdsFound = new LinkedHashSet<String>()
while (bareNumberMatcher.find()) {
    String n = bareNumberMatcher.group(1)
    try {
        if (Long.parseLong(n) >= 10000L) bareNumericIdsFound.add(n)
    } catch (NumberFormatException ignored) {}
}

// Find Long literals in getCustomFieldValue / setCustomFieldValue
Set<String> longLiteralIds = new LinkedHashSet<String>()
[Pattern.compile('setCustomFieldValue\\s*\\(\\s*(\\d+)L'),
 Pattern.compile('getCustomFieldValue\\s*\\(\\s*(\\d+)L')].each { Pattern p ->
    Matcher lm = p.matcher(SCRIPT_TO_FIX)
    while (lm.find()) longLiteralIds.add(lm.group(1))
}

// Find cf[XXXXX] JQL patterns
Set<String> cfJqlIds = new LinkedHashSet<String>()
Matcher cfJqlMatcher = Pattern.compile('cf\\[(\\d+)\\]').matcher(SCRIPT_TO_FIX)
while (cfJqlMatcher.find()) cfJqlIds.add(cfJqlMatcher.group(1))


// ============================================================
// SECTION 7 — RESOLVE ALL IDs TO CLOUD VALUES
//
// For every ID found, work out the best Cloud replacement.
// Track confidence: AUTO (certain), BEST_GUESS (ambiguous),
// UNRESOLVED (not found), or NO_CHANGE (already correct).
// ============================================================

// Each change recorded as a map with these keys:
//   dcValue, cloudValue, name, entityType, parent,
//   confidence, reason, warning

List<Map> changes = []
String modifiedScript = SCRIPT_TO_FIX

// --- customfield_XXXXX replacements ---
customFieldIdsFound.each { String dcFieldId ->
    List<MappingEntry> matches = idMapping.findAll { MappingEntry e ->
        e.entityType == 'CustomField' && e.dcId == dcFieldId
    }
    if (!matches) {
        changes << [
            dcValue:    dcFieldId,
            cloudValue: dcFieldId,
            name:       'unknown',
            entityType: 'CustomField',
            parent:     '',
            confidence: 'UNRESOLVED',
            reason:     'Not found in mapping — check your DC and Cloud exports are up to date',
            warning:    true
        ]
        return
    }
    MappingEntry entry = matches[0]
    if (entry.status == 'AMBIGUOUS') {
        changes << [
            dcValue:    dcFieldId,
            cloudValue: dcFieldId,
            name:       entry.name,
            entityType: 'CustomField',
            parent:     '',
            confidence: 'UNRESOLVED',
            reason:     "Duplicate field name '${entry.name}' — check your Cloud CSV and update manually".toString(),
            warning:    true
        ]
        return
    }
    if (entry.cloudId == 'UNRESOLVED') {
        changes << [
            dcValue:    dcFieldId,
            cloudValue: dcFieldId,
            name:       entry.name,
            entityType: 'CustomField',
            parent:     '',
            confidence: 'UNRESOLVED',
            reason:     "Field '${entry.name}' was not found on Cloud. It may not have migrated, or JCMA renamed it (e.g. added '(migrated)'). Check your Cloud CSV.".toString(),
            warning:    true
        ]
        return
    }
    modifiedScript = modifiedScript.replace(dcFieldId, entry.cloudId)
    changes << [
        dcValue:    dcFieldId,
        cloudValue: entry.cloudId,
        name:       entry.name,
        entityType: 'CustomField',
        parent:     '',
        confidence: 'AUTO',
        reason:     'Matched by field name in mapping',
        warning:    false
    ]
}

// --- Long literal field ID replacements ---
longLiteralIds.each { String numId ->
    List<MappingEntry> matches = idMapping.findAll { MappingEntry e ->
        e.entityType == 'CustomField' &&
        e.dcId == "customfield_${numId}".toString()
    }
    if (!matches) {
        changes << [
            dcValue:    "${numId}L".toString(),
            cloudValue: "${numId}L".toString(),
            name:       'unknown',
            entityType: 'CustomField',
            parent:     '',
            confidence: 'UNRESOLVED',
            reason:     'Not found in mapping — check your DC CSV',
            warning:    true
        ]
        return
    }
    MappingEntry entry = matches[0]
    String cloudNum = entry.cloudId.replace('customfield_', '')
    modifiedScript = modifiedScript.replaceAll(
        "(?<=getCustomFieldValue\\s{0,10}\\(\\s{0,10})${numId}L",
        "${cloudNum}L"
    )
    modifiedScript = modifiedScript.replaceAll(
        "(?<=setCustomFieldValue\\s{0,10}\\(\\s{0,10})${numId}L",
        "${cloudNum}L"
    )
    changes << [
        dcValue:    "${numId}L".toString(),
        cloudValue: "${cloudNum}L".toString(),
        name:       entry.name,
        entityType: 'CustomField',
        parent:     '',
        confidence: 'AUTO',
        reason:     'Matched by field name in mapping',
        warning:    false
    ]
}

// --- cf[XXXXX] JQL replacements ---
cfJqlIds.each { String numId ->
    List<MappingEntry> matches = idMapping.findAll { MappingEntry e ->
        e.entityType == 'CustomField' &&
        e.dcId == "customfield_${numId}".toString()
    }
    if (!matches) {
        changes << [
            dcValue:    "cf[${numId}]".toString(),
            cloudValue: "cf[${numId}]".toString(),
            name:       'unknown',
            entityType: 'CustomField',
            parent:     '',
            confidence: 'UNRESOLVED',
            reason:     'Not found in mapping — check your DC CSV',
            warning:    true
        ]
        return
    }
    MappingEntry entry = matches[0]
    String cloudNum = entry.cloudId.replace('customfield_', '')
    modifiedScript = modifiedScript.replace(
        "cf[${numId}]".toString(),
        "cf[${cloudNum}]".toString()
    )
    changes << [
        dcValue:    "cf[${numId}]".toString(),
        cloudValue: "cf[${cloudNum}]".toString(),
        name:       entry.name,
        entityType: 'CustomField',
        parent:     '',
        confidence: 'AUTO',
        reason:     'JQL field reference matched by field name in mapping',
        warning:    false
    ]
}

// --- Bare numeric ID replacements (options, statuses, projects etc) ---
bareNumericIdsFound.each { String numId ->
    // Skip if already handled as a Long literal or cf[] id
    if (longLiteralIds.contains(numId)) return
    if (cfJqlIds.contains(numId)) return

    List<MappingEntry> matches = idMapping.findAll { MappingEntry e ->
        e.entityType != 'CustomField' && e.dcId == numId
    }
    if (!matches) return

    // Pick best match based on context clues in the script
    MappingEntry best = null
    String reason     = ''

    // Context clue: inside contains() or optionMap comparison → option
    boolean looksLikeOption =
        SCRIPT_TO_FIX.contains("contains('${numId}')") ||
        SCRIPT_TO_FIX.contains("contains(\"${numId}\")") ||
        SCRIPT_TO_FIX.contains("== '${numId}'") ||
        SCRIPT_TO_FIX.contains("== \"${numId}\"") ||
        SCRIPT_TO_FIX.contains("[id: '${numId}']") ||
        SCRIPT_TO_FIX.contains("[id: \"${numId}\"]")

    // Context clue: inside project = in JQL → project
    boolean looksLikeProject =
        SCRIPT_TO_FIX.contains("project = ${numId}") ||
        SCRIPT_TO_FIX.contains("project=${numId}")

    if (looksLikeOption) {
        best   = matches.find { it.entityType == 'CustomFieldOption' }
        reason = "Chosen because it appears inside an option comparison in your script"
    } else if (looksLikeProject) {
        best   = matches.find { it.entityType == 'Project' }
        reason = "Chosen because it appears inside a project = clause in your script"
    }

    if (!best) {
        best   = matches[0]
        reason = matches.size() > 1
            ? "Best guess — multiple matches found, picked first. Please verify."
            : "Matched by ID in mapping"
    }

    String confidence = (matches.size() > 1 && !looksLikeOption && !looksLikeProject)
        ? 'BEST_GUESS'
        : 'AUTO'

    String cloudVal = best.cloudId == 'UNRESOLVED' ? numId : best.cloudId

    if (best.cloudId != 'UNRESOLVED') {
        modifiedScript = modifiedScript.replace(
            "'${numId}'".toString(), "'${cloudVal}'".toString()
        )
        modifiedScript = modifiedScript.replace(
            "\"${numId}\"".toString(), "\"${cloudVal}\"".toString()
        )
        modifiedScript = modifiedScript.replace(
            "project = ${numId}".toString(),
            "project = ${best.entityType == 'Project' && best.parent ? best.parent : cloudVal}".toString()
        )
    }

    String parentPart = best.parent ? " | ${best.parent}" : ''
    String allMatches = matches.size() > 1
        ? "\n         Other possible matches: ${matches.findAll { it != best }.collect { "${it.entityType}: ${it.name} → ${it.cloudId}" }.join(', ')}"
        : ''

    changes << [
        dcValue:    numId,
        cloudValue: cloudVal,
        name:       best.name,
        entityType: best.entityType,
        parent:     best.parent,
        confidence: confidence,
        reason:     reason + allMatches,
        warning:    best.cloudId == 'UNRESOLVED' || confidence == 'BEST_GUESS'
    ]
}


// ============================================================
// SECTION 8 — BUILD AND OUTPUT THE REPORT
// ============================================================

String W = ('═' * 64).toString()
String N = ('─' * 64).toString()

StringBuilder report = new StringBuilder()
report.append('\n').append(W).append('\n')
report.append('  SRM ID MAPPING TOOLKIT — FIND & REPLACE REPORT\n')
report.append(W).append('\n')

// Behaviour script warning
if (isBehaviourScript) {
    report.append('\n')
    report.append('  ⚠️  DC GROOVY BEHAVIOUR DETECTED\n')
    report.append(N).append('\n')
    report.append('  This is a DC Groovy Behaviour script.\n')
    report.append('  Cloud Behaviours use TypeScript — this script\n')
    report.append('  cannot be used on Cloud as-is.\n')
    report.append('\n')
    report.append('  What you need to do:\n')
    report.append('  1. Note which fields are referenced by name\n')
    report.append('  2. Look up their Cloud IDs in your Cloud CSV\n')
    report.append('  3. Rewrite the entire script as TypeScript\n')
    report.append('  4. Replace getFieldByName() with getFieldById()\n')
    report.append('  5. Replace setHidden(true/false) with setVisible(false/true)\n')
    report.append('\n')
    report.append('  What to do:\n')
    report.append('  1. Use the ScriptRunner Migration Suite (SMS) to convert\n')
    report.append('     this DC Groovy Behaviour to Cloud TypeScript.\n')
    report.append('  2. Once SMS has converted it, paste the TypeScript output\n')
    report.append('     back into this tool to fix the custom field IDs.\n')
    report.append('  3. Option IDs are not a concern — SMS uses option names,\n')
    report.append('     not numeric IDs, in TypeScript Behaviours.\n')
    report.append('\n')
    report.append('  → SMS:   https://migrationsuite.scriptrunnerhq.com/\n')
    report.append('  → Docs:  https://www.scriptrunnerhq.com/atlassian-apps/jira/scriptrunner-migration-suite\n')
    report.append(N).append('\n')
    logger.warn(report.toString())
    return
}

// Summary
boolean hasChanges    = changes.any { it.dcValue != it.cloudValue }
boolean hasWarnings   = changes.any { it.warning }
boolean nothingToDo   = changes.isEmpty()

if (nothingToDo) {
    report.append('\n  ✅  No DC IDs found in this script.\n')
    report.append('\n  Still check manually for:\n')
    report.append('    • Numbers followed by L e.g. setCustomFieldValue(11003L, ...)\n')
    report.append('    • JQL using cf[] syntax e.g. cf[11000]\n')
} else if (hasWarnings) {
    report.append('\n  ⚠️  Some IDs could not be resolved — see CHANGES below.\n')
    report.append('      Review carefully before using the updated script.\n')
} else {
    report.append('\n  ✅  All IDs resolved. Updated script is below.\n')
    report.append('      Please test thoroughly before using in production.\n')
}

// Changes section
report.append('\n').append(N).append('\n')
report.append('  📋  WHAT CHANGED — YOUR DC IDs VS CLOUD IDs\n')
report.append('  Review every line. Test the updated script before using it.\n')
report.append(N).append('\n')

if (changes) {
    changes.each { Map change ->
        String icon = change.confidence == 'AUTO'       ? '  ✅' :
                      change.confidence == 'BEST_GUESS' ? '  ⚠️ ' :
                                                          '  ❌'
        String label = change.confidence == 'AUTO'       ? 'AUTO-REPLACED' :
                       change.confidence == 'BEST_GUESS' ? 'BEST GUESS — PLEASE VERIFY' :
                                                           'UNRESOLVED — ACTION NEEDED'

        report.append("\n${icon} ${label}\n".toString())
        report.append("      DC value:    ${change.dcValue}\n".toString())
        report.append("      Cloud value: ${change.cloudValue}\n".toString())
        report.append("      Field/Entity: ${change.entityType} — ${change.name}".toString())
        if (change.parent) report.append(" | ${change.parent}".toString())
        report.append('\n')
        report.append("      Reason: ${change.reason}\n".toString())
    }
} else {
    report.append('  (no changes)\n')
}

// Updated script
report.append('\n').append(N).append('\n')
report.append('  🔧  YOUR UPDATED SCRIPT\n')
report.append('  ⚠️  This is our best attempt. You MUST test this script\n')
report.append('      before using it. Check every change listed above.\n')
report.append(N).append('\n')
report.append(modifiedScript.trim()).append('\n')

report.append('\n').append(W).append('\n')

logger.warn(report.toString())
