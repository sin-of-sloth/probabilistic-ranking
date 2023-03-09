# Additional Information on Relevance Models

Relevance Models are a classic probabilistic version of blind relevance feedback (BRF) based approach for language
modeling IR.

- Estimates a query language model `P ( t | q )` based on top results
- Assumes the top `k` ranked results by query likelihood (QL) as relevant

![RM Intro](https://github.com/sin-of-sloth/probabilistic-ranking/blob/master/relevance_models/assets/img/rm-1.png)

![RM1 Computation 1](https://github.com/sin-of-sloth/probabilistic-ranking/blob/master/relevance_models/assets/img/rm-2.png)

![RM1 Computation 2](https://github.com/sin-of-sloth/probabilistic-ranking/blob/master/relevance_models/assets/img/rm-3.png)

![RM3 Intro](https://github.com/sin-of-sloth/probabilistic-ranking/blob/master/relevance_models/assets/img/rm-4.png)

![RM3 Computation](https://github.com/sin-of-sloth/probabilistic-ranking/blob/master/relevance_models/assets/img/rm-5.png)

Re-rank documents by `P(Q*|D,R) --> A4 (Product of P(t | D, R))`, `t` is expanded query terms (query terms + expanded
top `n` terms from documents).

## Original Reference:
Lavrenko, V., & Croft, W. B. (2001). _Relevance based language models_. In Proceedings of the 24th annual international
ACM SIGIR conference on Research and development in information retrieval. SIGIR01: 24th ACM/SIGIR International
Conference on Research and Development in Information Retrieval. ACM. https://doi.org/10.1145/383952.383972
