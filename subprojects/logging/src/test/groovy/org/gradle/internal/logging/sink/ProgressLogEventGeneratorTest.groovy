/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.gradle.internal.logging.sink

import org.gradle.internal.logging.OutputSpecification
import org.gradle.internal.logging.events.OutputEventListener
import org.gradle.internal.logging.events.StyledTextOutputEvent

class ProgressLogEventGeneratorTest extends OutputSpecification {
    private final OutputEventListener target = Mock()

    // TODO(ew): Add test changes here for build operation ids

    def insertsLogHeaderForOperation() {
        ProgressLogEventGenerator generator = new ProgressLogEventGenerator(target, false)
        def startEvent = start(loggingHeader: 'description')
        def completeEvent = complete('status')

        when:
        generator.onOutput(startEvent)

        then:
        1 * target.onOutput(!null) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans[0].text == 'description'
            assert event.timestamp == startEvent.timestamp
            assert event.operationId == startEvent.operationId
        }
        0 * target._

        when:
        generator.onOutput(completeEvent)

        then:
        1 * target.onOutput(!null) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 3
            assert event.spans[0].text == toNative(' ')
            assert event.spans[1].text == toNative('status')
            assert event.spans[2].text == toNative('\n')
            assert event.timestamp == completeEvent.timestamp
            assert event.operationId == completeEvent.operationId
        }
        0 * target._
    }

    def insertsLogHeaderForOperationAndDeferredHeaderMode() {
        ProgressLogEventGenerator generator = new ProgressLogEventGenerator(target, true)
        def startEvent = startWithHeader('description')
        def completeEvent = complete('status')

        when:
        generator.onOutput(startEvent)

        then:
        0 * target._

        when:
        generator.onOutput(completeEvent)

        then:
        1 * target.onOutput(!null) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 3
            assert event.spans[0].text == toNative('description ')
            assert event.spans[1].text == toNative('status')
            assert event.spans[2].text == toNative('\n')
            assert event.timestamp == completeEvent.timestamp
            assert event.operationId == completeEvent.operationId
        }
        0 * target._
    }

    def insertsLogHeaderAndFooterWhenOperationGeneratesSomeOutput() {
        ProgressLogEventGenerator generator = new ProgressLogEventGenerator(target, false)
        def logEvent = event('message')

        when:
        generator.onOutput(startWithHeader('description'))

        then:
        1 * target.onOutput(!null) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 1
            assert event.spans[0].text == 'description'
        }
        0 * target._

        when:
        generator.onOutput(logEvent)

        then:
        1 * target.onOutput({it instanceof StyledTextOutputEvent}) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 1
            assert event.spans[0].text == toNative('\n')
        }
        1 * target.onOutput(logEvent)
        0 * target._

        when:
        generator.onOutput(complete('status'))

        then:
        1 * target.onOutput(!null) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 3
            assert event.spans[0].text == toNative('description ')
            assert event.spans[1].text == toNative('status')
            assert event.spans[2].text == toNative('\n')
        }
        0 * target._
    }

    def insertsLogHeaderAndFooterWhenOperationGeneratesSomeOutputAndInDeferredHeaderMode() {
        ProgressLogEventGenerator generator = new ProgressLogEventGenerator(target, true)
        def logEvent = event('message')
        def startEvent = startWithHeader('description')
        def completeEvent = complete('status')

        when:
        generator.onOutput(startEvent)

        then:
        0 * target._

        when:
        generator.onOutput(logEvent)

        then:
        1 * target.onOutput({it instanceof StyledTextOutputEvent}) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 1
            assert event.spans[0].text == toNative('description\n')
            assert event.timestamp == startEvent.timestamp
            assert event.operationId == startEvent.operationId
        }
        1 * target.onOutput(logEvent)
        0 * target._

        when:
        generator.onOutput(completeEvent)

        then:
        1 * target.onOutput(!null) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 3
            assert event.spans[0].text == toNative('description ')
            assert event.spans[1].text == toNative('status')
            assert event.spans[2].text == toNative('\n')
            assert event.operationId == completeEvent.operationId
        }
        0 * target._
    }

    def insertsLogMessagesWhenOperationsAreNested() {
        ProgressLogEventGenerator generator = new ProgressLogEventGenerator(target, false)

        when:
        generator.onOutput(startWithHeader('description'))

        then:
        1 * target.onOutput(!null) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 1
            assert event.spans[0].text == toNative('description')
        }
        0 * target._

        when:
        generator.onOutput(startWithHeader('description2'))

        then:
        1 * target.onOutput({it instanceof StyledTextOutputEvent && it.spans[0].text == toNative('\n')}) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 1
            assert event.spans[0].text == toNative('\n')
        }
        1 * target.onOutput(!null) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 1
            assert event.spans[0].text == toNative('description2')
        }
        0 * target._

        when:
        generator.onOutput(complete('status2'))

        then:
        1 * target.onOutput(!null) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 3
            assert event.spans[0].text == toNative(' ')
            assert event.spans[1].text == toNative('status2')
            assert event.spans[2].text == toNative('\n')
        }
        0 * target._

        when:
        generator.onOutput(complete('status'))

        then:
        1 * target.onOutput(!null) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 3
            assert event.spans[0].text == toNative('description ')
            assert event.spans[1].text == toNative('status')
            assert event.spans[2].text == toNative('\n')
        }
        0 * target._
    }

    def insertsLogMessagesWhenOperationsAreNestedAndInDeferredMode() {
        ProgressLogEventGenerator generator = new ProgressLogEventGenerator(target, true)

        when:
        generator.onOutput(startWithHeader('description'))
        generator.onOutput(startWithHeader('description2'))

        then:
        0 * target._

        when:
        generator.onOutput(complete('status2'))

        then:
        1 * target.onOutput({it instanceof StyledTextOutputEvent && it.spans[0].text == toNative('description\n')})
        1 * target.onOutput(!null) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 3
            assert event.spans[0].text == toNative('description2 ')
            assert event.spans[1].text == toNative('status2')
            assert event.spans[2].text == toNative('\n')
        }
        0 * target._

        when:
        generator.onOutput(complete('status'))

        then:
        1 * target.onOutput(!null) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 3
            assert event.spans[0].text == toNative('description ')
            assert event.spans[1].text == toNative('status')
            assert event.spans[2].text == toNative('\n')
        }
        0 * target._
    }

    def insertsLogHeaderForOperationWhoseLoggingHeaderIsNotTheSameAsShortDescriptionWhenDeferredMode() {
        ProgressLogEventGenerator generator = new ProgressLogEventGenerator(target, true)
        def startEvent = start(loggingHeader: 'header', shortDescription: 'short-description')
        def completeEvent = complete('status')

        when:
        generator.onOutput(startEvent)

        then:
        1 * target.onOutput(!null) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans[0].text == 'header'
            assert event.operationId == startEvent.operationId
        }
        0 * target._

        when:
        generator.onOutput(completeEvent)

        then:
        1 * target.onOutput(!null) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 3
            assert event.spans[0].text == toNative(' ')
            assert event.spans[1].text == toNative('status')
            assert event.spans[2].text == toNative('\n')
            assert event.timestamp == completeEvent.timestamp
            assert event.operationId == completeEvent.operationId
        }
        0 * target._
    }

    def insertsLogHeaderWhenOperationHasNoFinalStatus() {
        ProgressLogEventGenerator generator = new ProgressLogEventGenerator(target, false)

        when:
        generator.onOutput(startWithHeader('description'))

        then:
        1 * target.onOutput({it instanceof StyledTextOutputEvent && it.spans[0].text == 'description'})
        0 * target._

        when:
        generator.onOutput(complete(''))

        then:
        1 * target.onOutput({it instanceof StyledTextOutputEvent && it.spans[0].text == toNative('\n')})
        0 * target._
    }

    def insertsLogHeaderWhenOperationHasNoFinalStatusAndInDeferredMode() {
        ProgressLogEventGenerator generator = new ProgressLogEventGenerator(target, true)

        when:
        generator.onOutput(startWithHeader('description'))

        then:
        0 * target._

        when:
        generator.onOutput(complete(''))

        then:
        1 * target.onOutput({it instanceof StyledTextOutputEvent && it.spans[0].text == toNative('description\n')})
        0 * target._
    }

    def ignoresOperationsWithNoLogHeaderDefined() {
        ProgressLogEventGenerator generator = new ProgressLogEventGenerator(target, false)

        when:
        generator.onOutput(startWithHeader(''))

        then:
        0 * target._

        when:
        generator.onOutput(complete('status'))

        then:
        0 * target._
    }

    def ignoresOperationsWithNoLogHeaderDefinedWhenInDeferredMode() {
        ProgressLogEventGenerator generator = new ProgressLogEventGenerator(target, true)

        when:
        generator.onOutput(startWithHeader(''))

        then:
        0 * target._

        when:
        generator.onOutput(complete('status'))

        then:
        0 * target._
    }

    def insertsLogHeaderWhenOperationHasNoFinalStatusAndGeneratesLogMessages() {
        ProgressLogEventGenerator generator = new ProgressLogEventGenerator(target, false)
        def event = event('event')

        when:
        generator.onOutput(startWithHeader('description'))
        generator.onOutput(event)

        then:
        1 * target.onOutput({it instanceof StyledTextOutputEvent && it.spans[0].text == toNative('description')})
        1 * target.onOutput({it instanceof StyledTextOutputEvent && it.spans[0].text == toNative('\n')})
        1 * target.onOutput(event)
        0 * target._

        when:
        generator.onOutput(complete(''))

        then:
        0 * target._
    }

    def insertsLogHeaderWhenOperationHasNoFinalStatusAndGeneratesLogMessagesAndInDeferredMode() {
        ProgressLogEventGenerator generator = new ProgressLogEventGenerator(target, true)
        def event = event('event')

        when:
        generator.onOutput(startWithHeader('description'))
        generator.onOutput(event)

        then:
        1 * target.onOutput({it instanceof StyledTextOutputEvent && it.spans[0].text == toNative('description\n')})
        1 * target.onOutput(event)
        0 * target._

        when:
        generator.onOutput(complete(''))

        then:
        0 * target._
    }

    def insertsLogMessagesWhenOperationsAreNestedAndHaveNoDescriptionAndInDeferredMode() {
        ProgressLogEventGenerator generator = new ProgressLogEventGenerator(target, true)

        when:
        generator.onOutput(startWithHeader(''))
        generator.onOutput(startWithHeader('nested'))

        then:
        0 * target._

        when:
        generator.onOutput(complete('status2'))

        then:
        1 * target.onOutput(!null) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 3
            assert event.spans[0].text == toNative('nested ')
            assert event.spans[1].text == toNative('status2')
            assert event.spans[2].text == toNative('\n')
        }
        0 * target._

        when:
        generator.onOutput(complete('status'))

        then:
        0 * target._
    }

    def ignoresProgressEvents() {
        ProgressLogEventGenerator generator = new ProgressLogEventGenerator(target, false)

        when:
        generator.onOutput(startWithHeader('description'))
        generator.onOutput(progress('tick'))
        generator.onOutput(complete('status'))

        then:
        1 * target.onOutput({it instanceof StyledTextOutputEvent && it.spans[0].text == toNative('description')})
        1 * target.onOutput(!null) >> { args ->
            StyledTextOutputEvent event = args[0]
            assert event.spans.size() == 3
            assert event.spans[0].text == toNative(' ')
            assert event.spans[1].text == toNative('status')
            assert event.spans[2].text == toNative('\n')
        }
        0 * target._
    }

    def startWithHeader(String header) {
        return start(loggingHeader: header, shortDescription: header)
    }

    def handlesMultipleOperationsInProgressAtOnce() {
        ProgressLogEventGenerator generator = new ProgressLogEventGenerator(target, true)
        given:
        def firstStart = startWithHeader("task1")
        def secondStart = startWithHeader("task2")
        def secondProgress = progress("task2-progress")
        def secondComplete = complete("task2-done")
        def firstComplete = complete("task1-done")
        when:
        generator.onOutput(firstStart)
        generator.onOutput(secondStart)
        generator.onOutput(firstComplete)
        generator.onOutput(secondProgress)
        generator.onOutput(secondComplete)

        then:
        1 * target.onOutput({ StyledTextOutputEvent event ->
            event.spans.size() == 1 && event.spans[0].text == toNative('task2\n') && event.operationId == secondStart.operationId
        })
        1 * target.onOutput({ StyledTextOutputEvent event ->
            event.spans.size() == 3 &&
                event.spans[0].text == toNative('task1 ') &&
                event.spans[1].text == toNative('task1-done') &&
                event.spans[2].text == toNative('\n') &&
                event.operationId == firstStart.operationId
        })
        1 * target.onOutput({ StyledTextOutputEvent event ->
            event.spans.size() == 3 &&
                event.spans[0].text == toNative('task2 ') &&
                event.spans[1].text == toNative('task2-done') &&
                event.spans[2].text == toNative('\n') &&
                event.operationId == secondStart.operationId
        })
        0 * target._
    }
}
