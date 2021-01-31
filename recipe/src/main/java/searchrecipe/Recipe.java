package searchrecipe;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Recipe_table")
public class Recipe {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String recipeNm;
    private String cookingMethod;
    private String materialNm;
    private String qty;

    @PostPersist
    public void onPostPersist(){
        MaterialOrdered materialOrdered = new MaterialOrdered();
        BeanUtils.copyProperties(this, materialOrdered);
        materialOrdered.publishAfterCommit();


    }

    @PrePersist
    public void onPrePersist(){
        Searched searched = new Searched();
        BeanUtils.copyProperties(this, searched);
        searched.publishAfterCommit();


    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getRecipeNm() {
        return recipeNm;
    }

    public void setRecipeNm(String recipeNm) {
        this.recipeNm = recipeNm;
    }
    public String getCookingMethod() {
        return cookingMethod;
    }

    public void setCookingMethod(String cookingMethod) {
        this.cookingMethod = cookingMethod;
    }
    public String getMaterialNm() {
        return materialNm;
    }

    public void setMaterialNm(String materialNm) {
        this.materialNm = materialNm;
    }
    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }




}
