package repo.build.command

import repo.build.GitFeature
import repo.build.RepoBuild
import spock.lang.Specification

class StatusSpecification extends Specification {

    def setup() {
        GroovyMock(GitFeature, global: true)
    }

    def "without args"() {
        def repoBuild = new RepoBuild('status')

        when:
        repoBuild.execute()

        then:
        1 * GitFeature.status(_)
    }

    def "with args"() {
        def repoBuild = new RepoBuild('status', '-j', '2')

        when:
        repoBuild.execute()

        then:
        1 * GitFeature.status(_)
    }

}
