package utils

import org.gradle.api.Project
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Utils {
    static String getGitBranch() {
        String branch = ""

        if (System.getenv('GITHUB_HEAD_REF')) {
            branch = System.getenv('GITHUB_HEAD_REF')
        } else if (System.getenv('GITHUB_REF_NAME')) {
            branch = System.getenv('GITHUB_REF_NAME')
        }

        if (!branch) {
            def process = ['git', 'rev-parse', '--abbrev-ref', 'HEAD'].execute()
            process.waitFor()
            def output = process.text.trim()
            
            if (process.exitValue() == 0) {
                branch = output
            } else {
                branch = 'unknown'
            }
        }

        return branch.replaceAll('/', '_')
    }

    static String getFormattedDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd-HHmm"));
    }

    static String getBuildNumber() {
        if (System.getenv("GITHUB_RUN_NUMBER") != null) {
            return "r" + System.getenv("GITHUB_RUN_NUMBER")
        }
        return "local." + getFormattedDate()
    }
}
