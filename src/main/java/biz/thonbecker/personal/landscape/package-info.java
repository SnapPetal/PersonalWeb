/**
 * Landscape Planning Module
 *
 * This module provides landscape planning capabilities with plant selection based on USDA hardiness
 * zones. Users can upload images of their yard or 3D renders, receive AI-powered plant
 * recommendations, search for region-appropriate plants, and create annotated landscape plans.
 *
 * <h2>Public API</h2>
 * Other modules should interact with this module ONLY through:
 * <ul>
 *   <li>{@link biz.thonbecker.personal.landscape.platform.LandscapeService} - Main service implementation</li>
 *   <li>{@link biz.thonbecker.personal.landscape.api.LandscapePlan} - Domain model</li>
 *   <li>{@link biz.thonbecker.personal.landscape.api.PlantInfo} - Plant data model</li>
 *   <li>{@link biz.thonbecker.personal.landscape.api.HardinessZone} - Zone enumeration</li>
 * </ul>
 *
 * <h2>Internal Implementation</h2>
 * The {@code platform} package contains implementation details and should NOT be accessed directly
 * by other modules.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Image upload to S3 with CloudFront CDN delivery</li>
 *   <li>AI-powered plant recommendations using OpenAI vision</li>
 *   <li>USDA Plants Database integration for authoritative plant data</li>
 *   <li>Hardiness zone-based plant filtering</li>
 *   <li>Interactive plan creation with plant placement annotation</li>
 *   <li>Plan persistence and sharing</li>
 * </ul>
 *
 * @since 1.0
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Landscape Planning",
        allowedDependencies = {"shared", "user"})
@org.jspecify.annotations.NullMarked
package biz.thonbecker.personal.landscape;
