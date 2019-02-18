package au.unisa.erl.textparse.textexample.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;


@Controller
public class TextParserController {
    @RequestMapping(value="textparse", method= RequestMethod.GET)
    public String textparse(){
        return "textparse";
    }

    @RequestMapping(value="index", method = RequestMethod.GET)
    public String index(){
        return "index";
    }
}
