/**
 * Content Module
 *
 * <p>This module handles various content services including Bible verses, dad jokes, and
 * experiences.
 *
 * <p>Public API:
 * <ul>
 *   <li>{@link biz.thonbecker.personal.content.api.ContentFacade} - Main operations for content
 *       retrieval</li>
 *   <li>{@link biz.thonbecker.personal.content.domain.BibleVerse} - Bible verse domain model</li>
 *   <li>{@link biz.thonbecker.personal.content.domain.DadJoke} - Dad joke domain model</li>
 * </ul>
 *
 * <p>Module Dependencies:
 * <ul>
 *   <li>shared - For REST client and caching infrastructure</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Content Services",
        allowedDependencies = {"shared"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
@org.jspecify.annotations.NullMarked
package biz.thonbecker.personal.content;
