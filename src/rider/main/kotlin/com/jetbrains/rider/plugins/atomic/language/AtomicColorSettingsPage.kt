package com.jetbrains.rider.plugins.atomic.language

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class AtomicColorSettingsPage : ColorSettingsPage {
    
    override fun getIcon(): Icon = AtomicIcons.FILE
    
    override fun getHighlighter(): SyntaxHighlighter = AtomicSyntaxHighlighter()
    
    override fun getDemoText(): String = """
        # This is a comment
        header: EntityAPI
        entityType: IEntity
        aggressiveInlining: true
        
        namespace: SampleGame
        className: EntityAPI
        directory: Assets/Game/Scripts/Gameplay/Entity/
        
        imports:
        - UnityEngine
        - Atomic.Entities
        - System
        - Atomic.Elements
        - Modules.Gameplay
        - Atomic.Extensions
        - Gameplay.Entity.Common.TakeDamage
        
        tags:
        - Damageable
        - Moveable
        - Item
        - Melee
        - Weapon
        - Projectile
        - Character
        - Enemy
        - WeaponHolder
        
        values:
        
        # Main
        - GameObject: GameObject
        - Transform: Transform
        - Rigidbody: Rigidbody
        - VisualRoot: Transform
        - Collider: Collider
        
        # Movement  
        - MoveCondition: IExpression<bool>
        - MoveDirection: IReactiveVariable<Vector2>
        - MoveSpeed: IValue<float>
        
        # AI
        - StoppingDistance: IValue<float>
        
        # Rotation
        - AngularSpeed: IValue<float>
        - RotateCondition: IExpression<bool>
        
        # Aim
        - AimDirection: IReactiveVariable<Vector2>
        
        # Life
        - Health: Health
        - Lifetime: Cooldown
        - DestroyAction: IAction
        - DamageTakenEvent: IEvent<TakeDamageArgs>
        - DeathEvent: IEvent
        
        # Weapon
        - Owner: IEntity
        - CurrentWeapon: IEntity
    """.trimIndent()
    
    override fun getAdditionalHighlightingTagToDescriptorMap(): MutableMap<String, TextAttributesKey>? = null
    
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS
    
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    
    override fun getDisplayName(): String = "Atomic"
    
    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Keywords", AtomicSyntaxHighlighter.KEYWORD),
            AttributesDescriptor("Section headers", AtomicSyntaxHighlighter.SECTION),
            AttributesDescriptor("Property keys", AtomicSyntaxHighlighter.PROPERTY_KEY),
            AttributesDescriptor("Strings", AtomicSyntaxHighlighter.STRING),
            AttributesDescriptor("Booleans", AtomicSyntaxHighlighter.BOOLEAN),
            AttributesDescriptor("Identifiers", AtomicSyntaxHighlighter.IDENTIFIER),
            AttributesDescriptor("Types", AtomicSyntaxHighlighter.TYPE),
            AttributesDescriptor("Generic types", AtomicSyntaxHighlighter.GENERIC_TYPE),
            AttributesDescriptor("Operators", AtomicSyntaxHighlighter.OPERATOR),
            AttributesDescriptor("Comments", AtomicSyntaxHighlighter.COMMENT),
            AttributesDescriptor("Bad characters", AtomicSyntaxHighlighter.BAD_CHARACTER)
        )
    }
}