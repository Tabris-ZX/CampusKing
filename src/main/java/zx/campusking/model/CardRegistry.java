package zx.campusking.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CardRegistry {

    private List<CardDefinition> characters = new ArrayList<>();
    private List<CardDefinition> skills = new ArrayList<>();

}
