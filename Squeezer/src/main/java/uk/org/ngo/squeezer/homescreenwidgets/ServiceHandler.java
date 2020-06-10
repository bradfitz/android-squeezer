package uk.org.ngo.squeezer.homescreenwidgets;

import uk.org.ngo.squeezer.service.ISqueezeService;

@FunctionalInterface
interface ServiceHandler {
    void run(ISqueezeService service) throws Exception;
}
