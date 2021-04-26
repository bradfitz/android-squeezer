package uk.org.ngo.squeezer.homescreenwidgets;

import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.ISqueezeService;

@FunctionalInterface
interface ServicePlayerHandler {
    void run(ISqueezeService service, Player player) throws Exception;
}
