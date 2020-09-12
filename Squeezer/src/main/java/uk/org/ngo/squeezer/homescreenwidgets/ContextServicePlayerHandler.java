package uk.org.ngo.squeezer.homescreenwidgets;

import android.content.Context;

import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.ISqueezeService;

@FunctionalInterface
interface ContextServicePlayerHandler {
    void run(Context context, ISqueezeService service, Player player) throws Exception;
}
