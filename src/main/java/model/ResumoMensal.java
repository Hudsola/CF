package model;

public class ResumoMensal {
    private String mes;
    private double receita, investimentos, despesaTotal;
    private double alimentacao, moradia, educacao, pet, saude, transporte, pessoais, lazer, financeiros;
    private double saldo, saldoAcumulado;

    public ResumoMensal(String mes) { this.mes = mes; }

    public String getMes()                  { return mes; }
    public double getReceita()              { return receita; }
    public void setReceita(double v)        { receita = v; }
    public double getInvestimentos()        { return investimentos; }
    public void setInvestimentos(double v)  { investimentos = v; }
    public double getDespesaTotal()         { return despesaTotal; }
    public void setDespesaTotal(double v)   { despesaTotal = v; }
    public double getAlimentacao()          { return alimentacao; }
    public void setAlimentacao(double v)    { alimentacao = v; }
    public double getMoradia()              { return moradia; }
    public void setMoradia(double v)        { moradia = v; }
    public double getEducacao()             { return educacao; }
    public void setEducacao(double v)       { educacao = v; }
    public double getPet()                  { return pet; }
    public void setPet(double v)            { pet = v; }
    public double getSaude()                { return saude; }
    public void setSaude(double v)          { saude = v; }
    public double getTransporte()           { return transporte; }
    public void setTransporte(double v)     { transporte = v; }
    public double getPessoais()             { return pessoais; }
    public void setPessoais(double v)       { pessoais = v; }
    public double getLazer()                { return lazer; }
    public void setLazer(double v)          { lazer = v; }
    public double getFinanceiros()          { return financeiros; }
    public void setFinanceiros(double v)    { financeiros = v; }
    public double getSaldo()                { return saldo; }
    public void setSaldo(double v)          { saldo = v; }
    public double getSaldoAcumulado()       { return saldoAcumulado; }
    public void setSaldoAcumulado(double v) { saldoAcumulado = v; }
}
