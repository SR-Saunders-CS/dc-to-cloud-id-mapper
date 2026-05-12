// ============================================================
// SRM ID MAPPING TOOLKIT — DC EXPORT SCRIPT
// Run in: ScriptRunner Data Center > Script Console
//
// WHAT THIS SCRIPT DOES:
//   Exports all custom fields, field options, issue types,
//   statuses, priorities, resolutions, and projects from
//   your Jira Data Center instance as a CSV.
//
// HOW TO USE:
//   1. Run this script in ScriptRunner DC > Script Console
//   2. Click the LOGS tab (not the Result tab)
//   3. SCROLL TO THE VERY BOTTOM — the CSV is always last
//   4. You will see this line (ignore it, do not copy it):
//        WARN [...]: >>>>>>>>>> CSV START <<<<<<<<<<
//   5. Start copying from the NEXT line — the one starting with:
//        "EntityType","Name","ParentName","DC_ID","Status"
//   6. Keep copying until the last Project line
//   7. Stop when you see (ignore it, do not copy it):
//        WARN [...]: >>>>>>>>>> CSV END <<<<<<<<<<
//   8. Paste into a new file in VS Code or Cursor
//   9. Save as dc-export.csv
// ============================================================

import com.atlassian.jira.component.ComponentAccessor

List<List<String>> rows = [['EntityType', 'Name', 'ParentName', 'DC_ID', 'Status']]
def seenOptionIds = [] as Set<Long>

def fieldNameCounts = [:] as Map<String, Integer>
ComponentAccessor.customFieldManager.customFieldObjects.each { cf ->
    fieldNameCounts[cf.name] = (fieldNameCounts[cf.name] ?: 0) + 1
}

ComponentAccessor.customFieldManager.customFieldObjects.each { cf ->
    def fieldStatus = fieldNameCounts[cf.name] > 1 ? 'AMBIGUOUS' : 'OK'
    rows << (['CustomField', cf.name, '',
              "customfield_${cf.idAsLong}", fieldStatus] as List<String>)

    cf.getConfigurationSchemes().each { scheme ->
        scheme.getConfigs().values().each { config ->
            def optionNameCounts = [:] as Map<String, Integer>
            def options = ComponentAccessor.optionsManager.getOptions(config)
            options?.each { opt ->
                if (!opt.parentOptionId) {
                    optionNameCounts[opt.value] = (optionNameCounts[opt.value] ?: 0) + 1
                }
            }
            options?.each { option ->
                if (!option.parentOptionId && seenOptionIds.add(option.optionId)) {
                    def optStatus = optionNameCounts[option.value] > 1 ? 'AMBIGUOUS' : 'OK'
                    rows << (['CustomFieldOption', option.value, cf.name,
                              option.optionId.toString(), optStatus] as List<String>)
                    option.childOptions?.each { child ->
                        if (seenOptionIds.add(child.optionId)) {
                            rows << (['CustomFieldOption', child.value,
                                      "${cf.name} > ${option.value}".toString(),
                                      child.optionId.toString(), 'OK'] as List<String>)
                        }
                    }
                }
            }
        }
    }
}

ComponentAccessor.constantsManager.allIssueTypeObjects.each { issueType ->
    rows << (['IssueType', issueType.name, '', issueType.id, 'OK'] as List<String>)
}
ComponentAccessor.constantsManager.statuses.each { status ->
    rows << (['Status', status.name, '', status.id, 'OK'] as List<String>)
}
ComponentAccessor.constantsManager.priorities.each { priority ->
    rows << (['Priority', priority.name, '', priority.id, 'OK'] as List<String>)
}
ComponentAccessor.constantsManager.resolutions.each { resolution ->
    rows << (['Resolution', resolution.name, '', resolution.id, 'OK'] as List<String>)
}
ComponentAccessor.projectManager.projectObjects.each { project ->
    rows << (['Project', project.name, project.key,
              project.id.toString(), 'OK'] as List<String>)
}

def csv = rows.collect { row ->
    row.collect { cell ->
        "\"${(cell ?: '').replace('"', '""')}\""
    }.join(',')
}.join('\n')

log.warn('>>>>>>>>>> CSV START <<<<<<<<<<')
log.warn(csv)
log.warn('>>>>>>>>>> CSV END <<<<<<<<<<')
