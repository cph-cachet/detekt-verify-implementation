package dk.cachet.detekt.extensions

import dk.cachet.detekt.extensions.rules.DataClass
import dk.cachet.detekt.extensions.rules.Immutable
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider


class VerifyImplementationRuleSetProvider : RuleSetProvider
{
	override val ruleSetId: String = "verify-implementation"
	
	
	override fun instance( config: Config ): RuleSet = RuleSet(
		ruleSetId,
		listOf(
			DataClass( config ),
			Immutable( config )
		)
	)
}
