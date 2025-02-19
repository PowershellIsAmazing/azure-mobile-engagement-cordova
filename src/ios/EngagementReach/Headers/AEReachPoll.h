/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 */

#import <Foundation/Foundation.h>
#import "AEInteractiveContent.h"

/**
 * The `AEReachPoll` class defines objects that represent generic Engagement poll.
 *
 * You usually have to use this class when you implement your own poll
 * view controller.
 *
 * **See also**
 *
 * - <AEPollViewController>
 */
@interface AEReachPoll : AEInteractiveContent {
  @private
  NSArray* _questions;
  NSMutableDictionary* _answers;
}

/**
 * Poll questions.<br>
 * Contains <AEReachPollQuestion> objects.
 */
@property(readonly) NSArray* questions;

/**
 * Parse a poll
 * @param element Parsed XML root DOM element.
 * @result A new poll or nil if it couldn't be parsed.
 */
+ (id)pollWithElement:(AE_TBXMLElt*)element;

/**
 * Fill answer for a given question. Answers are sent when calling <[AEReachContent actionContent]>.
 * @param qid Question id as specified in <[AEReachPollQuestion questionId]>.
 * @param cid Choice id as specified in <[AEReachPollChoice choiceId]>.
 * @see questions
 */
- (void)fillAnswerWithQuestionId:(NSString*)qid choiceId:(NSString*)cid;

@end
