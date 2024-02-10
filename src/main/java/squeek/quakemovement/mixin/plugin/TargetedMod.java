package squeek.quakemovement.mixin.plugin;

import com.falsepattern.lib.mixin.ITargetedMod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Predicate;

import static com.falsepattern.lib.mixin.ITargetedMod.PredicateHelpers.*;

@RequiredArgsConstructor
public enum TargetedMod implements ITargetedMod {
    
;
    @Getter
    public final String modName;
    @Getter
    public final boolean loadInDevelopment;
    @Getter
    public final Predicate<String> condition;
}
