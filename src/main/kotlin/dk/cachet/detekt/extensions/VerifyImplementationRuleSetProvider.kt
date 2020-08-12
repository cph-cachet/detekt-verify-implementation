package dk.cachet.detekt.extensions

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider


class VerifyImplementationRuleSetProvider : RuleSetProvider
{
	override val ruleSetId: String = "verify-implementation"
	
	
	override fun instance( config: Config ): RuleSet = RuleSet( ruleSetId, emptyList() )
}
